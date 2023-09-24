package com.sdercolin.vlabeler.ui.dialog.customization

import com.sdercolin.vlabeler.model.LabelerConf

class LabelerItem(
    labelerConf: LabelerConf,
    disabled: Boolean,
) : CustomizableItem(
    name = labelerConf.name,
    author = labelerConf.author,
    version = labelerConf.version,
    displayedName = labelerConf.displayedName,
    description = labelerConf.description,
    email = labelerConf.email,
    website = labelerConf.website,
    rootFile = labelerConf.rootFile,
    canRemove = labelerConf.builtIn.not(),
    disabled = disabled,
)
