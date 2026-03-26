PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE "user" (
    "user_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "name" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "pw_hash" TEXT NOT NULL,
    "discount" INTEGER DEFAULT 0,
    "email_verified_flag" TEXT NOT NULL DEFAULT 'N',
    "create_date" TEXT NOT NULL,
    "update_date" TEXT,
    "verification_hash" TEXT,
    "reset_hash" TEXT,
    "free_trial_completed" TEXT NOT NULL DEFAULT 'N'
);
INSERT INTO "user" VALUES(1,'John Doe','john@example.com','$2a$10$hash',0,'Y','2024-01-15','2024-06-01',NULL,NULL,'N');
INSERT INTO "user" VALUES(2,'Jane Smith','jane@example.com','$2a$10$hash2',10,'Y','2024-02-20','2024-06-01',NULL,NULL,'Y');
CREATE TABLE "language" (
    "language_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "live" TEXT NOT NULL DEFAULT 'N',
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "origin" TEXT NOT NULL,
    "region" TEXT NOT NULL,
    "num_of_speakers" TEXT NOT NULL,
    "thumbnail_image_url" TEXT,
    "header_img_url" TEXT,
    "create_date" TEXT NOT NULL,
    "update_date" TEXT
);
CREATE TABLE "language_level" (
    "language_level_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "language_id" INTEGER NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "title" TEXT,
    "go_text" TEXT,
    "price" INTEGER NOT NULL,
    "start_page_text" TEXT NOT NULL,
    "finish_page_text" TEXT NOT NULL,
    "active" TEXT NOT NULL DEFAULT 'Y',
    "create_date" TEXT NOT NULL,
    "update_date" TEXT,
    "encouragement" TEXT,
    FOREIGN KEY("language_id") REFERENCES "language"("language_id")
);
CREATE TABLE "module" (
    "module_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "language_level_id" INTEGER NOT NULL,
    "video_content_id" INTEGER NOT NULL,
    "name" TEXT,
    "description" TEXT NOT NULL,
    "module_flow_index" INTEGER NOT NULL,
    "active" TEXT NOT NULL DEFAULT 'Y',
    "create_date" TEXT NOT NULL,
    "update_date" TEXT,
    FOREIGN KEY("language_level_id") REFERENCES "language_level"("language_level_id"),
    FOREIGN KEY("video_content_id") REFERENCES "video_content"("video_content_id")
);
CREATE TABLE "activity" (
    "activity_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "module_id" INTEGER NOT NULL,
    "activity_table" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "active" TEXT NOT NULL DEFAULT 'Y',
    "create_date" TEXT NOT NULL,
    "update_date" TEXT,
    FOREIGN KEY("module_id") REFERENCES "module"("module_id") ON DELETE CASCADE
);
CREATE TABLE "video_content" (
    "video_content_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "title" TEXT NOT NULL,
    "media_url" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "create_date" TEXT NOT NULL,
    "update_date" TEXT
);
CREATE TABLE "settings" (
    "settings_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "key" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "create_date" TEXT NOT NULL,
    "update_date" TEXT
);
CREATE TABLE "word_bank" (
    "word_bank_id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "module_id" INTEGER,
    "target_word" TEXT,
    "working_word" TEXT,
    FOREIGN KEY("module_id") REFERENCES "module"("module_id") ON DELETE CASCADE
);
CREATE TABLE "type_test" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "label" VARCHAR(255) NOT NULL,
    "score" REAL,
    "amount" NUMERIC(10,2),
    "is_active" INTEGER DEFAULT 1,
    "raw_data" BLOB
);
DELETE FROM "sqlite_sequence";
INSERT INTO "sqlite_sequence" VALUES('user',215);
INSERT INTO "sqlite_sequence" VALUES('language',15);
CREATE INDEX "idx_user_email" ON "user" ("email");
CREATE INDEX "idx_activity_module" ON "activity" ("module_id");
COMMIT;
