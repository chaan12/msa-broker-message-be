ALTER TABLE product_retry_jobs
    ADD COLUMN description TEXT,
    ADD COLUMN quantity INTEGER,
    ADD COLUMN image TEXT,
    ADD COLUMN category VARCHAR(255),
    ADD COLUMN subcategory VARCHAR(255),
    ADD COLUMN brand VARCHAR(255),
    ADD COLUMN supplier VARCHAR(255);
