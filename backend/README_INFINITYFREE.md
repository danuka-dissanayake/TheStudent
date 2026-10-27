# InfinityFree MySQL & PHP Backend Setup Guide for TheStudent

This backend powers the dynamic **Past Papers** catalog for the TheStudent Android application.

## Directory Structure
```
backend/
├── database.sql       <- Run this in InfinityFree phpMyAdmin
├── config.php         <- Database credentials (edit this)
├── api/
│   └── get_papers.php <- The JSON API queried by the Android App
├── admin/
│   └── index.php      <- Visual web panel to upload, add, edit, and delete papers
└── uploads/           <- Folder for uploaded PDF papers & schemes
```

---

## 1. Import Database to InfinityFree
1. Log in to your **InfinityFree Client Area** and go to **Control Panel (vPanel)**.
2. Under the **Databases** section, open **MySQL Databases**.
3. Create a new database named `thestudent` (or note your generated database name, e.g. `epiz_12345678_thestudent`).
4. Note your credentials shown on that page:
   - **MySQL Host:** (e.g. `sql108.infinityfree.com` or `sql202.epizy.com`)
   - **MySQL Username:** (e.g. `epiz_12345678`)
   - **Password:** (your vPanel account password)
   - **Database Name:** (e.g. `epiz_12345678_thestudent`)
5. Click **phpMyAdmin** next to the database name.
6. In phpMyAdmin, click the **Import** tab at the top.
7. Choose the `database.sql` file and click **Go**.
   - This creates the `past_papers` table with all fields (`id`, `grade`, `category`, `subject`, `stream`, `year`, `exam_type`, `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`, `file_size`, `pages`, `created_at`, `status`) and inserts initial seed papers.

> **Note for Existing Database:** If you already created your `past_papers` table previously without the `stream` column, simply run this one-line query in phpMyAdmin under the **SQL** tab to preserve all your existing records:
> ```sql
> ALTER TABLE `past_papers` ADD COLUMN `stream` VARCHAR(100) NULL AFTER `subject`, ADD INDEX `idx_stream` (`stream`);
> ```

---

## 2. Upload Files to InfinityFree
1. In the InfinityFree Control Panel, open **Online File Manager** (or connect via FTP with FileZilla).
2. Open the **`htdocs/`** directory.
3. Upload:
   - `config.php`
   - The `api/` folder (with `get_papers.php`)
   - The `admin/` folder (with `index.php`)
   - Create an empty `uploads/` folder inside `htdocs/` with permissions 755.

---

## 3. Configure Database Connection
Open `config.php` in the File Manager (or before uploading) and fill in your details:

```php
$DB_HOST = 'sql108.infinityfree.com'; // Your MySQL Host from InfinityFree
$DB_NAME = 'epiz_xxxxxxx_thestudent';    // Your Database Name
$DB_USER = 'epiz_xxxxxxx';            // Your MySQL Username
$DB_PASS = 'your_account_password';   // Your vPanel Password
```

---

## 4. Test the API in Your Browser
Open:
`https://yourdomain.infinityfreeapp.com/api/get_papers.php`

You should receive a clean JSON response containing the past papers:
```json
{
  "status": "success",
  "count": 11,
  "data": [ ... ]
}
```

---

## 5. Web Admin Panel
Open:
`https://yourdomain.infinityfreeapp.com/admin/index.php`

From this panel, you can:
- **Upload PDFs** or paste external PDF URLs
- **Add new papers** with Grade (6–13), Category (Grade 6–10, O/L, A/L), Subject, Year, Medium, and Marking Scheme
- **Edit or Delete papers**
- **Publish / Unpublish papers** with a single click

When you add or update papers in MySQL, they **instantly appear in the Android App**!
