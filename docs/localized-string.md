# Localized strings in vLabeler

In scripts for JSON definition files of labelers and plugins, a localized string can be used instead of a plain string.

A localized string is an object with language codes as keys and strings as values. e.g.

```javascript
// in a script
let myLocalizedDescription = {
    "en": "This is a description in English.",
    "zh": "这是中文的描述。",
    "ja": "これは日本語の説明です。"
}
```

```
// inside a JSON
{
    ...,
    "description": {
        "en": "This is a description in English.",
        "zh": "这是中文的描述。",
        "ja": "これは日本語の説明です。"
    },
    ...
}
```

A localized string can always be assigned by a plain string, which is treated as the default language `en`.

```javascript
// in a script
let myLocalizedDescription = "This is a description in English."

// this is equivalent to

let myLocalizedDescription = {
    "en": "This is a description in English."
}
```

When displaying a localized string, vLabeler will choose the string with the language code that matches the current
language code in the application settings, or the default language `en` if no match is found, using startsWith matching.

e.g. If the current language code is `en-US`, the string with key `en` will be used. So it's recommended to use common
language codes like `en` and `zh` instead of `en-US` and `zh-CN` to cover more cases.

Please note that you have to provide a default language `en` in your localization map, otherwise the plugin gets a
parse error when being loaded.
