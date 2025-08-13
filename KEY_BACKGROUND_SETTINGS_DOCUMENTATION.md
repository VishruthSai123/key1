# Key Background Settings Feature Documentation

## Overview
A new customization feature has been added to NeoBoard AI Keyboard that allows users to independently control the appearance of individual keys, separate from the main keyboard theme.

## Feature Description
Users can now choose from three distinct key background styles:

### 1. Dark (Default)
- **ID**: `dark`
- **Description**: Dark gray keys with the original styling
- **Text Color**: White (#FFFFFF)
- **Use Case**: Works well with all keyboard themes, provides good contrast

### 2. Light White
- **ID**: `light_white`
- **Description**: Clean white background with subtle shadows
- **Text Color**: Dark gray (#333333)
- **Use Case**: Perfect for bright environments or users who prefer light interfaces

### 3. Light Transparent
- **ID**: `light_transparent`
- **Description**: Semi-transparent with subtle opacity
- **Text Color**: Medium gray (#444444)
- **Use Case**: Allows keyboard theme gradients to show through while maintaining key definition

## Technical Implementation

### Data Structure
- **KeyBackgroundStyle.kt**: Data class defining background styles
- **KeyBackgroundStyles.kt**: Object containing all available styles and utility functions

### Drawable Resources
- `modern_key_button.xml` - Original dark style
- `modern_key_button_light_white.xml` - Light white style
- `modern_key_button_light_transparent.xml` - Light transparent style

### Settings Integration
- Added to Settings Activity with radio button selection
- Preferences stored in SharedPreferences as `key_background_style`
- Real-time updates when keyboard is shown (via `onStartInputView`)

### Keyboard Service Integration
- Added to `SimpleKeyWiseInputMethodService.kt`
- Applies to all text input keys (letters, numbers, symbols, space)
- Excludes special keys (shift, enter, backspace) which maintain their unique styling
- Automatically adjusts text color based on background style

## User Interface
The key background setting appears in the Settings screen as a dedicated section:
- **Title**: "Key Background Style"
- **Description**: "Customize the appearance of individual keys"
- **Options**: Radio buttons for each style with name and description

## Key Features
1. **Independent from Themes**: Key backgrounds can be changed without affecting the main keyboard theme
2. **Automatic Text Color**: Text color automatically adjusts for optimal readability
3. **Comprehensive Coverage**: Applies to all letter, number, symbol, and space keys
4. **Real-time Updates**: Changes apply immediately when keyboard is shown
5. **Persistent Settings**: User preference is saved and restored across app sessions

## Benefits
- **Enhanced Customization**: Users have more granular control over keyboard appearance
- **Accessibility**: Light options provide better visibility in bright environments
- **Style Flexibility**: Transparent option allows theme gradients to show through
- **User Preference**: Accommodates different visual preferences and lighting conditions

## Usage Instructions
1. Open NeoBoard Settings
2. Navigate to "Key Background Style" section
3. Select desired style (Dark, Light White, or Light Transparent)
4. Return to keyboard - changes apply automatically

## Future Enhancements
- Additional style options (colored backgrounds, patterns)
- Per-key customization
- Dynamic adjustment based on ambient light
- User-defined custom styles

This feature enhances the keyboard's customization capabilities while maintaining the existing theme system, providing users with more control over their typing experience. 