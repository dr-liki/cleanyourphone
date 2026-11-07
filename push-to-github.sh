#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/dr-liki/cleanyourphone.git"
DESC="Debloat your Android phone on-device without root using Shizuku or Wireless ADB — powered by UAD-ng's uad_lists.json with TVSlim-inspired UI and reversible pm disable-user --user 0 backup & restore."

git init -b main
git add .
git commit -m "feat: initial CleanYourPhone Android app + signed APK release workflow"
git tag -a v1.0.0 -m "CleanYourPhone v1.0.0"

if command -v gh >/dev/null 2>&1; then
  gh repo create dr-liki/cleanyourphone --public --description "$DESC" --source=. --remote=origin --push
  git push origin v1.0.0
  gh repo edit dr-liki/cleanyourphone --add-topic android,debloater,shizuku,wireless-adb,jetpack-compose,kotlin,uad-ng,bloatware-removal,privacy
  echo "Pushed main + tag v1.0.0! GitHub Actions is now building and releasing CleanYourPhone-v1.0.0.apk."
else
  git remote add origin "$REPO_URL"
  git push -u origin main
  git push origin v1.0.0
fi
