# 🔧 KeyWise AI - Troubleshooting Guide

## 🚨 Common Issues and Solutions

### 1. **Android Emulator Crash**

**Problem**: Emulator crashes when testing the keyboard
**Solutions**:
- ✅ **Use the Simple Version**: We've created `SimpleKeyWiseInputMethodService` which uses XML layouts instead of Compose for better stability
- ✅ **Increase Emulator Resources**: Allocate more RAM (4GB+) and storage to your AVD
- ✅ **Use Real Device**: Test on a physical Android device for best results
- ✅ **Cold Boot**: Wipe emulator data and start fresh

### 2. **Keyboard Not Appearing**

**Problem**: KeyWise keyboard doesn't show up in keyboard selection
**Solutions**:
- ✅ **Check Manifest**: Ensure `SimpleKeyWiseInputMethodService` is registered in AndroidManifest.xml
- ✅ **Enable in Settings**: Go to Settings > System > Languages & input > Virtual keyboard > Manage keyboards
- ✅ **Restart App**: Force close and restart the app you're testing in
- ✅ **Reboot Device**: Sometimes a device restart is needed

### 3. **Build Errors**

**Problem**: Compilation fails
**Solutions**:
- ✅ **Clean Build**: Run `./gradlew clean` then `./gradlew build`
- ✅ **Update Dependencies**: Ensure all dependencies are up to date
- ✅ **Check SDK**: Verify Android SDK 35 is installed
- ✅ **Gradle Sync**: Sync project with Gradle files

### 4. **AI Features Not Working**

**Problem**: AI buttons don't process text
**Solutions**:
- ✅ **API Key**: Ensure OpenAI API key is set correctly
- ✅ **Internet Connection**: Check device has internet access
- ✅ **Daily Limit**: Free tier has 15 actions/day limit
- ✅ **Text Selection**: Try typing some text first
- ✅ **Check Logs**: Look for error messages in logcat

### 5. **Memory Issues**

**Problem**: App crashes due to memory
**Solutions**:
- ✅ **Use Simple Service**: Switch to `SimpleKeyWiseInputMethodService`
- ✅ **Optimize Images**: Reduce image sizes if any
- ✅ **Close Other Apps**: Free up device memory
- ✅ **Test on Higher-End Device**: Use device with more RAM

## 🛠️ Debugging Commands

### Check Logcat for Errors
```bash
adb logcat -s KeyWise
adb logcat | grep -i "error\|crash\|exception"
```

### Clear App Data
```bash
adb shell pm clear com.vishruth.key1
```

### Force Stop App
```bash
adb shell am force-stop com.vishruth.key1
```

### List Input Methods
```bash
adb shell ime list -s
```

### Enable KeyWise Keyboard via ADB
```bash
adb shell ime enable com.vishruth.key1/.keyboard.SimpleKeyWiseInputMethodService
adb shell ime set com.vishruth.key1/.keyboard.SimpleKeyWiseInputMethodService
```

## 🔄 Testing Strategy

1. **Start Simple**: Test basic typing first
2. **Test AI Actions**: Try each AI button individually
3. **Check Limits**: Verify daily usage tracking
4. **Different Apps**: Test in various apps (Messages, Notes, etc.)
5. **Network Issues**: Test with/without internet

## 🎯 Best Practices

- **Use Real Device**: For final testing, always use a physical device
- **Monitor Memory**: Watch for memory leaks during extended use
- **Test Edge Cases**: Empty text, very long text, special characters
- **User Feedback**: Collect feedback on real-world usage
- **Gradual Rollout**: Start with limited users before full release

## 📱 Supported Configurations

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 35)
- **RAM**: 2GB+ recommended
- **Storage**: 100MB+ free space
- **Network**: Required for AI features

## 🚀 Performance Tips

- **Background Processing**: AI calls run in background threads
- **Error Handling**: All operations have try-catch blocks
- **Resource Cleanup**: Coroutines are properly cancelled
- **Memory Management**: Views are properly recycled

## 📞 Getting Help

If you're still experiencing issues:

1. **Check GitHub Issues**: Look for similar problems
2. **Create New Issue**: Provide device info, logs, and steps to reproduce
3. **Join Community**: Discord/Telegram for real-time help
4. **Contact Support**: Email for critical issues

---

**Last Updated**: December 2024  
**Version**: 1.0.0 