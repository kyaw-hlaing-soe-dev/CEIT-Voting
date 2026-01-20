#!/bin/bash

echo "=== KTU Voting App - Database & Image Verification ==="
echo ""

echo "1. Checking Docker containers..."
docker ps --filter name=ktuvoting --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "2. Checking database candidates count..."
docker exec ktuvoting-postgres psql -U ktuvote_user ktuvoting -t -c "SELECT category, COUNT(*) FROM candidates GROUP BY category ORDER BY category" 2>/dev/null || echo "Database query failed"
echo ""

echo "3. Checking total candidates..."
TOTAL=$(docker exec ktuvoting-postgres psql -U ktuvote_user ktuvoting -t -c "SELECT COUNT(*) FROM candidates" 2>/dev/null | tr -d ' ')
echo "Total candidates in database: $TOTAL (should be 45)"
echo ""

echo "4. Checking image files in source..."
SOURCE_COUNT=$(ls /Users/aunghtet/Desktop/projects/test2/src/main/resources/static/images/*.jpg 2>/dev/null | grep -E "(king|queen|prince|princess|couple)[0-9]\.jpg" | wc -l | tr -d ' ')
echo "Candidate images in source: $SOURCE_COUNT (should be 45)"
echo ""

echo "5. Checking image files in build target..."
TARGET_COUNT=$(ls /Users/aunghtet/Desktop/projects/test2/target/classes/static/images/*.jpg 2>/dev/null | grep -E "(king|queen|prince|princess|couple)[0-9]\.jpg" | wc -l | tr -d ' ')
echo "Candidate images in target: $TARGET_COUNT (should be 45)"
echo ""

echo "6. Sample candidates with image URLs..."
docker exec ktuvoting-postgres psql -U ktuvote_user ktuvoting -t -c "SELECT category, candidate_number, name, image_url FROM candidates WHERE candidate_number <= 2 ORDER BY category, candidate_number" 2>/dev/null || echo "Database query failed"
echo ""

echo "=== Verification Complete ==="
echo ""
echo "Next steps:"
echo "1. Open http://localhost:8080 in your browser"
echo "2. Enter a PIN to start voting"
echo "3. Verify all candidate photos load correctly"

