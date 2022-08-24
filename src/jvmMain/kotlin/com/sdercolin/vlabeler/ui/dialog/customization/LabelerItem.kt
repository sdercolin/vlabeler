package com.sdercolin.vlabeler.ui.dialog.customization

import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.CustomLabelerDir

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
    rootFile = CustomLabelerDir.resolve(labelerConf.fileName),
    canRemove = labelerConf.isBuiltIn.not(),
    disabled = disabled,
)
