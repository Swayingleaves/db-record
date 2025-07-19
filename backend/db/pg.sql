
-- 创建 book 表
CREATE TABLE book (
                      id SERIAL PRIMARY KEY,                          -- 自增主键
                      title VARCHAR(255) NOT NULL,                    -- 书名
                      author VARCHAR(255) NOT NULL,                   -- 作者
                      publisher VARCHAR(255),                         -- 出版社
                      publish_date DATE,                              -- 出版日期
                      isbn VARCHAR(20) UNIQUE,                        -- ISBN
                      category VARCHAR(100),                          -- 图书分类
                      price NUMERIC(10, 2),                           -- 图书价格
                      stock INTEGER DEFAULT 0,                        -- 库存数量
                      description TEXT,                               -- 图书描述
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 更新时间
);

-- 表注释
COMMENT ON TABLE book IS '图书信息表';

-- 字段注释
COMMENT ON COLUMN book.id IS '主键，自增ID';
COMMENT ON COLUMN book.title IS '书名';
COMMENT ON COLUMN book.author IS '作者';
COMMENT ON COLUMN book.publisher IS '出版社';
COMMENT ON COLUMN book.publish_date IS '出版日期';
COMMENT ON COLUMN book.isbn IS '图书ISBN，唯一';
COMMENT ON COLUMN book.category IS '图书分类';
COMMENT ON COLUMN book.price IS '图书价格（单位：元）';
COMMENT ON COLUMN book.stock IS '库存数量';
COMMENT ON COLUMN book.description IS '图书描述/简介';
COMMENT ON COLUMN book.created_at IS '记录创建时间';
COMMENT ON COLUMN book.updated_at IS '记录更新时间';


CREATE INDEX idx_book_title ON book(title);
CREATE INDEX idx_book_author ON book(author);
