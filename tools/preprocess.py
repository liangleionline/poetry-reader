#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""chinese-poetry 数据预处理：把精选经典文集规范成带索引+FTS5 的 poetry.db
输出：poetry.db（放进 Android app assets，App 首次启动拷贝到私有目录只读使用）"""
import json, os, re, sqlite3, sys, unicodedata

SRC = os.path.join(os.path.dirname(__file__), "..", "data-src")
OUT = os.path.join(os.path.dirname(__file__), "poetry.db")

# 每个文集的元信息: (collection_name, type, dynasty, [json路径...] 或 目录+匹配)
# type: 诗/词/文/经
COLLECTIONS = [
    ("唐诗三百首", "诗", "唐", ["全唐诗/唐诗三百首.json"]),
    ("唐诗补录",   "诗", "唐", ["全唐诗/唐诗补录.json"]),
    ("宋词三百首", "词", "宋", ["宋词/宋词三百首.json"]),
    ("诗经",       "经", "先秦", ["诗经/shijing.json"]),
    ("楚辞",       "诗", "战国", ["楚辞/chuci.json"]),
    ("论语",       "经", "先秦", ["论语/lunyu.json"]),
    ("元曲",       "曲", "元", ["元曲/yuanqu.json"]),
    ("花间集",     "词", "五代", ["五代诗词/huajianji/huajianji-1-juan.json",
                                 "五代诗词/huajianji/huajianji-2-juan.json",
                                 "五代诗词/huajianji/huajianji-3-juan.json",
                                 "五代诗词/huajianji/huajianji-4-juan.json",
                                 "五代诗词/huajianji/huajianji-5-juan.json",
                                 "五代诗词/huajianji/huajianji-6-juan.json",
                                 "五代诗词/huajianji/huajianji-7-juan.json",
                                 "五代诗词/huajianji/huajianji-8-juan.json",
                                 "五代诗词/huajianji/huajianji-9-juan.json",
                                 "五代诗词/huajianji/huajianji-x-juan.json"]),
    ("南唐词",     "词", "五代", ["五代诗词/nantang/poetrys.json"]),
    ("曹操诗集",   "诗", "东汉", ["曹操诗集/caocao.json"]),
    ("幽梦影",     "文", "清", ["幽梦影/youmengying.json"]),
    ("水墨唐诗",   "诗", "唐", ["水墨唐诗/shuimotangshi.json"]),
    ("纳兰性德",   "词", "清", ["纳兰性德/纳兰性德诗集.json"]),
    ("大学",       "经", "先秦", ["四书五经/daxue.json"]),
    ("中庸",       "经", "先秦", ["四书五经/zhongyong.json"]),
    ("孟子",       "经", "先秦", ["四书五经/mengzi.json"]),
]

# 蒙学：一个目录下多文件，逐一作为独立文集
MENGXUE_FILES = [
    ("三字经", "蒙学/sanzijing-new.json"),
    ("百家姓", "蒙学/baijiaxing.json"),
    ("千字文", "蒙学/qianziwen.json"),
    ("弟子规", "蒙学/dizigui.json"),
    ("声律启蒙", "蒙学/shenglvqimeng.json"),
    ("增广贤文", "蒙学/zengguangxianwen.json"),
    ("朱子家训", "蒙学/zhuzijiaxun.json"),
    ("文字蒙求", "蒙学/wenzimengqiu.json"),
    ("幼学琼林", "蒙学/youxueqionglin.json"),
    ("千家诗", "蒙学/qianjiashi.json"),
    ("古文观止", "蒙学/guwenguanzhi.json"),
]

def norm_ws(s):
    if not s:
        return ""
    s = s.replace("\u3000", " ").replace("\r", "")
    # 折叠多空白
    s = re.sub(r"[ \t]+", " ", s)
    return s.strip()

def clean_lines(lines):
    """把段落数组清洗成正文文本（每行一段），过滤空行/纯标点"""
    out = []
    if not isinstance(lines, list):
        return ""
    for ln in lines:
        if not isinstance(ln, str):
            continue
        ln = norm_ws(ln)
        ln = re.sub(r"[，。；：、！？\s,.;:!?]+$", "", ln)
        if not ln:
            continue
        out.append(ln)
    return "\n".join(out)

def get_title(o):
    for k in ("title", "rhythmic", "name"):
        if o.get(k):
            return str(o[k]).strip()
    return ""

def get_author(o):
    for k in ("author",):
        if o.get(k):
            return str(o[k]).strip()
    return "佚名"

def get_body(o):
    for k in ("paragraphs", "content", "para"):
        v = o.get(k)
        if v:
            if isinstance(v, str):
                # 长字符串：先按换行拆分
                return clean_lines(v.splitlines())
            return clean_lines(v)
    return ""

def extract(items, collection_name, ptype, dynasty, add_collection):
    if isinstance(items, dict):
        _handle_item(items, collection_name, ptype, dynasty, add_collection)
    elif isinstance(items, list):
        for it in items:
            _handle_item(it, collection_name, ptype, dynasty, add_collection)

def _handle_item(it, cname, ptype, dynasty, add_collection):
    if isinstance(it, list):
        for sub in it:
            _handle_item(sub, cname, ptype, dynasty, add_collection)
        return
    if not isinstance(it, dict):
        return
    # 论语式：{chapter, paragraphs} 且无 title/content —— 每条拆成独立条目
    if "chapter" in it and "paragraphs" in it and "title" not in it and "content" not in it:
        chap = it.get("chapter", "")
        for p in it.get("paragraphs", []):
            b = clean_lines([p])
            if b:
                add_collection(f"{cname}·{chap}", b, ptype, dynasty, cname, "")
        return
    # 容器式：content 为 dict 列表（可能多层嵌套）—— 递归展开
    if "content" in it and isinstance(it.get("content"), list) and it["content"] \
            and all(isinstance(x, dict) for x in it["content"]):
        for x in it["content"]:
            _handle_item(x, cname, ptype, dynasty, add_collection)
        return
    # 常规条目
    title = get_title(it)
    body = get_body(it)
    if not title:
        # 幽梦影式：仅 content 纯文本
        if isinstance(it.get("content"), str):
            b = clean_lines(it["content"].splitlines())
            if b:
                add_collection(cname, b, ptype, dynasty, cname, "")
        elif body:
            # 标题为空但有正文（如唐诗补录）—— 用首行兜底
            first = body.split("\n")[0][:24]
            add_collection(first, body, ptype, dynasty, cname, get_author(it))
        return
    if body:
        add_collection(title, body, ptype, dynasty, cname, get_author(it))

def main():
    if os.path.exists(OUT):
        os.remove(OUT)
    conn = sqlite3.connect(OUT)
    cur = conn.cursor()
    cur.execute("PRAGMA journal_mode=OFF")
    cur.execute("PRAGMA synchronous=OFF")
    cur.executescript("""
    CREATE TABLE poet(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        dynasty TEXT,
        intro TEXT
    );
    CREATE TABLE poem(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        poet_id INTEGER,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        type TEXT,
        source TEXT
    );
    CREATE TABLE collection(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE
    );
    CREATE TABLE poem_collection(
        poem_id INTEGER NOT NULL,
        collection_id INTEGER NOT NULL,
        PRIMARY KEY(poem_id, collection_id)
    );
    """)
    poets = {}          # name -> id
    poems = {}          # (title, author, body) -> id  (去重)
    coll_ids = {}
    counts = {"poem": 0, "poet": 0, "dup": 0, "empty": 0}

    def get_poet(name, dynasty):
        if name not in poets:
            cur.execute("INSERT INTO poet(name, dynasty) VALUES(?,?)", (name, dynasty))
            poets[name] = cur.lastrowid
            counts["poet"] += 1
        return poets[name]

    def get_coll(name):
        if name not in coll_ids:
            cur.execute("INSERT INTO collection(name) VALUES(?)", (name,))
            coll_ids[name] = cur.lastrowid
        return coll_ids[name]

    def add(title, body, ptype, dynasty, collname, author, chap=""):
        author = author or "佚名"
        body = (body or "").strip()
        title = (title or "").strip()
        if not body or not title:
            counts["empty"] += 1
            return
        key = (title, author, body[:200])
        if key in poems:
            # 同一作品归入多个文集时，只补充关联，不重复存正文
            pid = poems[key]
            counts["dup"] += 1
        else:
            pid = poems[key] = None  # placeholder
            poet_id = get_poet(author, dynasty)
            cur.execute("INSERT INTO poem(poet_id,title,content,type,source) VALUES(?,?,?,?,?)",
                        (poet_id, title, body, ptype, collname))
            pid = cur.lastrowid
            poems[key] = pid
            counts["poem"] += 1
        cid = get_coll(collname)
        cur.execute("INSERT OR IGNORE INTO poem_collection(poem_id,collection_id) VALUES(?,?)", (pid, cid))

    def add_collection(title, body, ptype, dynasty, collname, author):
        add(title, body, ptype, dynasty, collname, author)

    # 处理主要文集
    for cname, ptype, dynasty, paths in COLLECTIONS:
        for rel in paths:
            fp = os.path.join(SRC, rel)
            if not os.path.exists(fp):
                print("MISS", rel)
                continue
            items = json.load(open(fp, encoding="utf-8"))
            before = counts["poem"]
            extract(items, cname, ptype, dynasty, add_collection)
            print(f"  {cname} ({rel}): +{counts['poem']-before} poems")

    # 处理蒙学
    for cname, rel in MENGXUE_FILES:
        fp = os.path.join(SRC, rel)
        if not os.path.exists(fp):
            print("MISS", rel)
            continue
        items = json.load(open(fp, encoding="utf-8"))
        before = counts["poem"]
        extract(items, cname, "蒙", "宋", add_collection)
        print(f"  {cname}: +{counts['poem']-before} poems")

    cur.execute("CREATE INDEX idx_poem_title ON poem(title)")
    cur.execute("CREATE INDEX idx_poem_poet ON poem(poet_id)")
    cur.execute("CREATE INDEX idx_poem_type ON poem(type)")
    cur.execute("CREATE INDEX idx_poet_name ON poet(name)")
    cur.execute("CREATE INDEX idx_pc_coll ON poem_collection(collection_id)")

    # FTS5 全文检索（rowid 复用 poem.id）
    cur.executescript("""
    CREATE VIRTUAL TABLE fts_poem USING fts5(title, author, content, tokenize='unicode61');
    """)
    cur.execute("SELECT id, title, content, poet_id FROM poem")
    fts_rows = []
    for pid, title, content, poet_id in cur.fetchall():
        aname = ""
        if poet_id:
            cur.execute("SELECT name FROM poet WHERE id=?", (poet_id,))
            r = cur.fetchone()
            if r: aname = r[0]
        fts_rows.append((pid, title, aname, content))
    cur.executemany("INSERT INTO fts_poem(rowid, title, author, content) VALUES(?,?,?,?)", fts_rows)

    conn.commit()
    cur.execute("VACUUM")
    conn.commit()
    # 汇总
    cur.execute("SELECT COUNT(*) FROM poem"); n_poem = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM poet"); n_poet = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM collection"); n_coll = cur.fetchone()[0]
    conn.close()
    print("=" * 50)
    print(f"poems={n_poem} poets={n_poet} collections={n_coll}")
    print(f"during: poem={counts['poem']} dup_links={counts['dup']} empty_skip={counts['empty']}")
    sz = os.path.getsize(OUT)
    print(f"db size = {sz/1024/1024:.2f} MB -> {OUT}")

if __name__ == "__main__":
    main()
