-- QueryCraft MSSQL Test Database Generation Script
-- Purpose: Create a database with one large table (60,000 rows) for testing.

-- 1. Create Database (if not exists)
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'QueryCraftTest')
BEGIN
    PRINT 'Creating database QueryCraftTest...';
    CREATE DATABASE QueryCraftTest;
END
GO

USE QueryCraftTest;
GO

-- 2. Create Table without constraints (easy to delete)
IF OBJECT_ID('LargeDataTest', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping existing table LargeDataTest...';
    DROP TABLE LargeDataTest;
END
GO

PRINT 'Creating table LargeDataTest...';
CREATE TABLE LargeDataTest (
    ID INT IDENTITY(1,1) PRIMARY KEY,
    DataName NVARCHAR(100),
    Category NVARCHAR(50),
    Value FLOAT,
    CreatedAt DATETIME DEFAULT GETDATE(),
    Description NVARCHAR(MAX)
);
GO

-- 3. Populate with 60,000 rows efficiently using Cross Join
PRINT 'Inserting 60,000 rows... (This may take a few seconds)';
SET NOCOUNT ON;
DECLARE @TargetRows INT = 60000;

INSERT INTO LargeDataTest (DataName, Category, Value, Description)
SELECT TOP (@TargetRows)
    'Record_' + CAST(ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) AS NVARCHAR(10)) as DataName,
    CASE 
        WHEN ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) % 4 = 0 THEN 'IT'
        WHEN ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) % 4 = 1 THEN 'Marketing'
        WHEN ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) % 4 = 2 THEN 'Sales'
        ELSE 'HR' 
    END as Category,
    RAND(CHECKSUM(NEWID())) * 5000 as Value,
    'Synthetic test record for performance and deletion verification. Unique ID: ' + CAST(NEWID() AS NVARCHAR(50)) as Description
FROM sys.all_objects a
CROSS JOIN sys.all_objects b; -- Provides enough rows for 60,000 limit
GO

-- 4. Summary display
PRINT 'Generation complete!';
SELECT 
    (SELECT COUNT(*) FROM LargeDataTest) as TotalRows,
    (SELECT TOP 1 DataName FROM LargeDataTest ORDER BY ID DESC) as LastRecord;
GO
