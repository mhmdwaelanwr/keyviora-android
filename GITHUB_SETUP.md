# Publish KEYVIORA to GitHub

## Create empty repository (one-time)

GitHub → **New repository** → owner **mhmdwaelanwr** → name **keyviora-android**. Choose **Private** for initial device testing; do **not** initialize with README, license or `.gitignore` because they are already included.

Repo description: `KEYVIORA — One Touch. Full Control. | Native Android assistive controls (Kotlin)`

## Push from your machine

```bash
cd Keyviora-Android
git init -b main
git add .
git commit -m "feat: initial KEYVIORA Android release candidate"
git remote add origin https://github.com/mhmdwaelanwr/keyviora-android.git
git push -u origin main
```

Then **GitHub → Actions → KEYVIORA — Android CI**. Watch for a successful green run and download the APK artifact. If Actions need enabling on the repo, enable them in the GitHub UI.

## Android Studio

File → New → Project from Version Control → paste `https://github.com/mhmdwaelanwr/keyviora-android.git` → Clone. Build after Gradle sync.
