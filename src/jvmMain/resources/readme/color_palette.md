# How to create a customized color palette for vLabeler

Here is an example of a valid file for a customized color palette:

```json
{
    "name": "Sunset",
    "items": [
        {
            "color": "#FF000000",
            "weight": 0.0
        },
        {
            "color": "#FF02063E",
            "weight": 4.0
        },
        {
            "color": "#FFF21E07",
            "weight": 5.0
        },
        {
            "color": "#FFEDED0C",
            "weight": 1.0
        },
        {
            "color": "#FFFCFEF0",
            "weight": 0.5
        }
    ]
}
```

- Make sure the file is a valid JSON file, and the file name ends with `.json`.
    - Do not use `.example.json` to end the file name. This is only for example files and will not be loaded.

- `name` is the name of the color palette.
    - It will be displayed in the color palette selection menu.
    - It should not be duplicated with other color palettes, or the built-in color palettes.

- `items` are key colors in the color palette. In every `item`:
    - `color` is the color hex string in the format of `#AARRGGBB`.
        - `AA` is the alpha channel, `RR` is the red channel, `GG` is the green channel, and `BB` is the blue channel.
        - `AA` is optional. If it is not specified, it will be set to `FF` by default.
    - `weight` is the weight of the color.
        - It represents the weight of color gradation between this item and the previous item.
        - The weight of the first color should always be `0.0`.

- Please check other `.example.json` files in the same folders for more examples.

- You have to reopen the preferences dialog to trigger reloading the color palette files.

- The built-in color palette items cannot be modified or deleted.

- If your customized color palette is not loaded, please check the error log file to see what is wrong.

- Feel free to share your customized color palette with others.
    - If you feel confident, you can contact us to add your color palette to the built-in color palettes.
