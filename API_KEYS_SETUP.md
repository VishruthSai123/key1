# API Keys Setup Guide

## Overview
This project uses secure API key management to protect sensitive credentials. API keys are stored in `local.properties` which is automatically excluded from version control.

## Setup Instructions

### 1. API Keys Configuration
Create or update the `local.properties` file in the project root with your API keys:

```properties
# Gemini API Keys
GEMINI_PRIMARY_API_KEY=your_primary_gemini_api_key_here
GEMINI_BACKUP_API_KEY_1=your_backup_gemini_api_key_1_here
GEMINI_BACKUP_API_KEY_2=your_backup_gemini_api_key_2_here

# GPT-5 API Key
GPT5_API_KEY=your_gpt5_api_key_here
```

### 2. Security Features
- ✅ API keys are stored in `local.properties` (not committed to version control)
- ✅ Keys are accessed through `BuildConfig` at runtime
- ✅ Fallback validation prevents empty keys from breaking the app
- ✅ Error handling for missing or invalid configurations

### 3. Important Notes
- **Never commit `local.properties` to version control**
- The file is already included in `.gitignore`
- If keys are missing, the app will show a clear error message
- All functionality is preserved - only the storage method has changed

### 4. For New Developers
1. Clone the repository
2. Add your API keys to `local.properties` (create the file if it doesn't exist)
3. Build and run the project normally

### 5. API Key Sources
- **Gemini API**: Get from [Google AI Studio](https://makersuite.google.com/app/apikey)
- **GPT-5 API**: Get from [AI/ML API](https://aimlapi.com/)

## Build Process
The build system automatically:
1. Reads API keys from `local.properties`
2. Injects them securely into `BuildConfig`
3. Makes them available to the application at runtime
4. Validates that required keys are present

This ensures maximum security while maintaining full functionality.
