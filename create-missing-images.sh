#!/bin/bash

# Script to create missing candidate images
# This copies existing king/queen images as placeholders for missing images
# Replace these with actual photos later!

cd "$(dirname "$0")/src/main/resources/static/images"

echo "Creating missing PRINCE images (4-9)..."
cp king4.jpg prince4.jpg
cp king5.jpg prince5.jpg
cp king6.jpg prince6.jpg
cp king7.jpg prince7.jpg
cp king8.jpg prince8.jpg
cp king9.jpg prince9.jpg

echo "Creating missing PRINCESS images (4-9)..."
cp queen4.jpg princess4.jpg
cp queen5.jpg princess5.jpg
cp queen6.jpg princess6.jpg
cp queen7.jpg princess7.jpg
cp queen8.jpg princess8.jpg
cp queen9.jpg princess9.jpg

echo "Creating missing COUPLE images (1-9)..."
cp king1.jpg couple1.jpg
cp queen1.jpg couple2.jpg
cp king2.jpg couple3.jpg
cp queen2.jpg couple4.jpg
cp king3.jpg couple5.jpg
cp queen3.jpg couple6.jpg
cp king4.jpg couple7.jpg
cp queen4.jpg couple8.jpg
cp king5.jpg couple9.jpg

echo ""
echo "✅ All missing images created!"
echo ""
echo "⚠️  IMPORTANT: These are placeholder images!"
echo "   Replace them with actual candidate photos before going live."
echo ""
echo "Image count verification:"
ls -1 king*.jpg 2>/dev/null | wc -l | xargs -I {} echo "  KING: {} images"
ls -1 queen*.jpg 2>/dev/null | wc -l | xargs -I {} echo "  QUEEN: {} images"
ls -1 prince*.jpg 2>/dev/null | wc -l | xargs -I {} echo "  PRINCE: {} images"
ls -1 princess*.jpg 2>/dev/null | wc -l | xargs -I {} echo "  PRINCESS: {} images"
ls -1 couple*.jpg 2>/dev/null | wc -l | xargs -I {} echo "  COUPLE: {} images"

