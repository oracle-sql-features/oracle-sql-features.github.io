#!/bin/bash

echo "🔄 Generating navigation"
java .github/scripts/generate_navigation.java "$(pwd)"

echo ""
echo "📦 Installing dependencies"
echo "Please wait..."
npm install

echo ""
echo "🛠️  Building site"
echo "Please wait..."
npx antora local-playbook.yml
