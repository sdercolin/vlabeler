# How to setup custom font families

We provide 2 ways to setup custom font families:

1. Copy the font files to this folder.

If you want to use ttc font file with multiple fonts inside, you need to unpack it first.
For example using tools like: https://transfonter.org/ttc-unpack
Rarely, the metadata of each font in the ttc file is not correct, in this case you can switch to the second way.

2. Add a JSON file in this folder to define a font family.

The file name needs to end with `.font.json`.
Here is an example of the content of the JSON file:

```json
{
    "name": "My Font Family",
    "fonts": [
        {
            "path": "MyFont-Regular.ttf",
            "weight": 400,
            "isItalic": false
        },
        {
            "path": "MyFont-Bold.ttf",
            "weight": 700,
            "isItalic": false
        },
        {
            "path": "MyFont-Italic.ttf",
            "weight": 400,
            "isItalic": true
        },
        {
            "path": "MyFont-BoldItalic.ttf",
            "weight": 700,
            "isItalic": true
        }
    ]
}
```   

Note that:

- `name` is the name of the font family.
  It should not be duplicated with other font families including the built-in ones.
- `path` could be an absolute path or a relative path to this folder.
- `weight` is the font weight represented by a number.
  It could be `100`, `200`, `300`, `400`, `500`, `600`, `700`, `800`, `900`.
  If it is not specified, the value from the font metadata will be used.
- `isItalic` is a boolean value.
  If it is not specified, the value from the font metadata will be used.
   
