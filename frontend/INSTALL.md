# Installation Instructions

## Quick Fix for Tailwind CSS Error

Run this command in the frontend directory:

```powershell
npm install
```

This will install:
- tailwindcss
- autoprefixer  
- postcss
- All other dependencies

## After Installation

Once `npm install` completes successfully, run:

```powershell
npm run dev
```

The frontend should start without errors.

## If npm install fails

1. Delete `node_modules` folder (if exists)
2. Delete `package-lock.json` (if exists)
3. Run `npm install` again
4. If still failing, try `npm cache clean --force` then `npm install`
