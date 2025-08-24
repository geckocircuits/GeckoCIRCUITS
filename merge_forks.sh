#!/bin/bash

# Add remotes for the forks (if not already added)
git remote add metalab https://github.com/metalab-projects/GeckoCIRCUITS.git || echo "Remote 'metalab' already exists"
git remote add hkruenaegel https://github.com/HKruenaegel/GeckoCIRCUITS.git || echo "Remote 'hkruenaegel' already exists"

# Fetch all branches from the forks
git fetch metalab
git fetch hkruenaegel

# List remote branches so you can choose which to merge
echo "Branches from metalab-projects:"
git branch -r | grep metalab/
echo "Branches from HKruenaegel:"
git branch -r | grep hkruenaegel/

# Example: Merge a specific branch from metalab (replace 'branchname' with the actual branch)
# git merge metalab/branchname

# Example: Merge a specific branch from HKruenaegel (replace 'branchname' with the actual branch)
# git merge hkruenaegel/branchname

echo "To merge, use: git merge <remote>/<branch>"