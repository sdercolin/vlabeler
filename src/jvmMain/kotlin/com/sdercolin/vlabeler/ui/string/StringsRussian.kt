package com.sdercolin.vlabeler.ui.string

import com.sdercolin.vlabeler.ui.string.Strings.*

fun Strings.ru(): String = when (this) {
    AppName -> "vLabeler"
    MenuFile -> "Файл"
    MenuFileNewProject -> "Новый проект..."
    MenuFileOpen -> "Открыть..."
    MenuFileOpenRecent -> "Открыть недавние"
    MenuFileOpenRecentClear -> "Очистить список недавно открытых"
    MenuFileQuickEdit -> "Быстрое редактирование"
    MenuFileSave -> "Сохранить"
    MenuFileSaveAs -> "Сохранить как..."
    MenuFileProjectSetting -> "Настройки проекта..."
    MenuFileImportProject -> "Импортировать проект..."
    MenuFileReloadLabelFile -> "Перезагрузить файл меток"
    MenuFileReloadLabelFilePickFile -> "Из файла..."
    MenuFileReloadLabelFileDefault -> "Из выходного файла..."
    MenuFileReloadLabelFileDefaultWithoutConfirmation -> "Из выходного файла (без подтверждения)"
    MenuFileExport -> "Экспортировать файл лейбов..."
    MenuFileExportOverwrite -> "Экспортировать файл меток с заменой"
    MenuFileExportOverwriteAll -> "Экспортировать все файлы меток с заменой"
    MenuFileInvalidateCaches -> "Сбросить кэш"
    MenuFileClose -> "Закрыть"
    MenuEdit -> "Правка"
    MenuEditUndo -> "Отменить"
    MenuEditRedo -> "Повторить"
    MenuEditTools -> "Инструменты"
    MenuEditToolsCursor -> "Курсор"
    MenuEditToolsScissors -> "Ножницы"
    MenuEditToolsPan -> "Панорамирование"
    MenuEditToolsPlayback -> "Воспроизведение"
    MenuEditRenameEntry -> "Переименовать текущую запись..."
    MenuEditDuplicateEntry -> "Дублировать текущую запись..."
    MenuEditRemoveEntry -> "Удалить текущую запись..."
    MenuEditMoveEntry -> "Переместить текущую запись в..."
    MenuEditToggleDone -> "Переключить статус «Готово» для текущей записи"
    MenuEditToggleStar -> "Переключить статус «Избранное» для текущей записи"
    MenuEditEditTag -> "Изменить тег текущей записи"
    MenuEditEditEntryExtra -> "Изменить дополнительную информацию текущей записи"
    MenuEditMultipleEditMode -> "Редактировать все связанные записи"
    MenuEditEditModuleExtra -> "Изменить дополнительную информацию текущего подпроекта"
    MenuView -> "Вид"
    MenuViewToggleMarker -> "Показать контроллеры параметров"
    MenuViewPinEntryList -> "Закрепить список записей"
    MenuViewPinEntryListLocked -> "Заблокировать разделитель для закрепленного списка записей"
    MenuViewToggleProperties -> "Показать свойства"
    MenuViewToggleToolbox -> "Показать панель инструментов"
    MenuViewToggleTimescaleBar -> "Показать линейку шкалы времени"
    MenuViewOpenSampleList -> "Список сэмплов"
    MenuViewVideo -> "Отобразить связанное видео"
    MenuViewVideoOff -> "Выкл."
    MenuViewVideoEmbedded -> "Встроенное"
    MenuViewVideoNewWindow -> "В новом окне"
    MenuNavigate -> "Навигация"
    MenuNavigateOpenLocation -> "Открыть расположения"
    MenuNavigateOpenLocationRootDirectory -> "Корневой каталог сэмплов"
    MenuNavigateOpenLocationModuleDirectory -> "Каталог сэмплов текущего подпроекта"
    MenuNavigateOpenLocationProjectLocation -> "Расположение файла проекта"
    MenuNavigateNextEntry -> "Перейти к следующей записи"
    MenuNavigatePreviousEntry -> "Перейти к предыдущей записи"
    MenuNavigateNextSample -> "Перейти к следующему сэмплу"
    MenuNavigatePreviousSample -> "Перейти к предыдущему сэмплу"
    MenuNavigateJumpToEntry -> "Перейти к записи..."
    MenuNavigateNextModule -> "Перейти к следующему подпроекту"
    MenuNavigatePreviousModule -> "Перейти к предыдущему подпроекту"
    MenuNavigateJumpToModule -> "Перейти к подпроекту..."
    MenuNavigateScrollFit -> "Прокрутить для отображения текущей записи"
    MenuTools -> "Инструменты"
    MenuToolsBatchEdit -> "Пакетное редактирование"
    MenuToolsBatchEditQuickLaunchManager -> "Настройки слотов..."
    MenuToolsBatchEditQuickLaunch -> "Слот %d: %s"
    MenuToolsBatchEditShowDisabledItems -> "Показать плагины, которые нельзя использовать в текущем проекте"
    MenuToolsBatchEditManagePlugins -> "Управление плагинами..."
    MenuToolsPrerender -> "Предварительный рендеринг всех графиков..."
    MenuToolsSyncSample -> "Преобразовать все значения относительно конца сэмпла..."
    MenuToolsRecycleMemory -> "Освободить память"
    MenuToolsFileNameNormalizer -> "Нормализатор имен файлов..."
    MenuSettings -> "Настройки"
    MenuSettingsPreferences -> "Параметры..."
    MenuSettingsLabelers -> "Разметчики..."
    MenuSettingsTemplatePlugins -> "Генераторы шаблонов..."
    MenuSettingsTracking -> "Отслеживать использование приложения..."
    MenuHelp -> "Справка"
    MenuHelpCheckForUpdates -> "Проверить наличие обновлений..."
    MenuHelpOpenLogDirectory -> "Открыть каталог логов"
    MenuHelpIncludeInfoLog -> "Включить детальное логирование"
    MenuHelpOpenHomePage -> "Открыть домашнюю страницу vLabeler"
    MenuHelpOpenLatestRelease -> "Открыть последний релиз"
    MenuHelpOpenGitHub -> "Открыть страницу GitHub"
    MenuHelpJoinDiscord -> "Присоединиться к Discord"
    MenuHelpAbout -> "О программе"
    CommonOkay -> "ОК"
    CommonApply -> "Применить"
    CommonCancel -> "Отмена"
    CommonYes -> "Да"
    CommonNo -> "Нет"
    CommonWarning -> "Предупреждение"
    CommonError -> "Ошибка"
    CommonDetails -> "Подробности"
    CommonOthers -> "Прочее"
    CommonPrevious -> "Предыдущий"
    CommonNext -> "Следующий"
    CommonFinish -> "Готово"
    CommonSelect -> "Выбрать"
    CommonOpen -> "Открыть"
    CommonSave -> "Сохранить"
    CommonReset -> "Сбросить"
    CommonClear -> "Очистить"
    CommonInputErrorPromptNumber -> "Пожалуйста, введите число."
    CommonInputErrorPromptInteger -> "Пожалуйста, введите целое число."
    CommonInputErrorPromptNumberRange -> "Пожалуйста, введите число между %s и %s."
    CommonInputErrorPromptNumberMin -> "Пожалуйста, введите число, большее или равное %s."
    CommonInputErrorPromptNumberMax -> "Пожалуйста, введите число, меньшее или равное %s."
    CommonRootModuleName -> "(Корень)"
    StarterStart -> "Начать"
    StarterNewProject -> "Новый проект..."
    StarterOpen -> "Открыть..."
    StarterQuickEdit -> "Быстрое редактирование"
    StarterRecent -> "Недавние"
    StarterRecentEmpty -> "У вас нет недавних проектов."
    StarterRecentDeleted -> "Этот файл проекта был удален."
    StarterNewSampleDirectory -> "Каталог сэмплов"
    StarterNewWorkingDirectory -> "Расположение проекта"
    StarterNewProjectTitle -> "Новый проект"
    StarterNewProjectName -> "Название проекта"
    StarterNewProjectNameWarning -> "Файл проекта уже существует. Создание проекта перезапишет файл."
    StarterNewCacheDirectory -> "Каталог кэша"
    StarterNewLabelerCategory -> "Категория"
    StarterNewLabeler -> "Разметчик"
    StarterNewTemplatePlugin -> "Генератор шаблонов"
    StarterNewTemplatePluginNone -> "Нет"
    StarterNewInputFile -> "Входной файл (.%s)"
    StarterNewEncoding -> "Кодировка"
    StarterNewAutoExport -> "Автоэкспорт"
    StarterNewAutoExportHelp ->
        "Автоматически экспортировать проект с перезаписью указанного входного файла (если не указан, " +
            "используется расположение по умолчанию, определенное разметчиком) при сохранении проекта."
    StarterNewWarningSelfConstructedLabelerWithTemplatePlugin ->
        "Вы пытаетесь использовать генератор шаблонов с разметчиком, который управляет несколькими подпроектами. " +
            "Это опасно, так как может перезаписать все ваши существующие файлы меток к сгенерированными записями. " +
            "Пожалуйста, дважды подтвердите, что настройки соответствуют вашим потребностям."
    StarterNewDirectoryPage -> "Настройки каталогов"
    StarterNewLabelerPage -> "Настройки разметчика"
    StarterNewDataSourcePage -> "Настройки источника данных"
    StarterNewContentType -> "Создать с помощью..."
    StarterNewContentTypeDefault -> "По умолчанию"
    StarterNewContentTypeFile -> "Файл"
    StarterNewContentTypePlugin -> "Генератор шаблонов"
    StarterNewAdvancedSettings -> "Дополнительные настройки"
    SampleListIncludedHeader -> "Файлы сэмплов проекта"
    SampleListIncludedItemEntryCountSingle -> "%d запись"
    SampleListIncludedItemEntryCountPlural -> "%d записей"
    SampleListExcludedHeader -> "Другие файлы сэмплов"
    SampleListExcludedPlaceholder -> "В каталоге сэмплов нет неиспользуемых файлов."
    SampleListEntryHeader -> "Записи"
    SampleListEntriesPlaceholderUnselected -> "Выберите файл сэмпла слева, чтобы показать привязанные к нему записи."
    SampleListEntriesPlaceholderNoEntry -> "К выбранному файлу сэмпла не привязаны записи."
    SampleListEntriesPlaceholderNoEntryButton -> "Создать по умолчанию"
    SampleListCreateDefaultForAllButton -> "Создать по умолчанию для всех неиспользуемых"
    SampleListJumpToSelectedEntryButton -> "Перейти к выбранной записи"
    SampleListOpenSampleDirectoryButton -> "Открыть каталог сэмплов"
    SampleListCurrentModuleLabel -> "Подпроект: "
    SampleListSampleDirectoryLabel -> "Каталог сэмплов: "
    SampleListSampleDirectoryRedirectButton -> "Изменить каталог сэмплов"
    PrerendererModuleText -> "Рендеринг подпроектов %d/%d..."
    PrerendererModuleTextFinished -> "Рендеринг подпроектов %d/%d... Готово"
    PrerendererSampleText -> "Рендеринг файлов сэмплов %d/%d..."
    PrerendererSampleTextFinished -> "Рендеринг файлов сэмплов %d/%d... Готово"
    PrerendererChartText -> "Рендеринг графиков %d/%d..."
    PrerendererChartTextFinished -> "Рендеринг графиков %d/%d... Готово"
    EditorRenderStatusLabel -> "%d/%d Рендеринг..."
    ChooseSampleDirectoryDialogTitle -> "Выбрать каталог сэмплов"
    ChooseWorkingDirectoryDialogTitle -> "Выбрать каталог расположения проекта"
    ChooseCacheDirectoryDialogTitle -> "Выбрать каталог кэша"
    ChooseInputFileDialogTitle -> "Выбрать входной файл"
    OpenProjectDialogTitle -> "Открыть проект"
    SaveAsProjectDialogTitle -> "Сохранить проект как"
    ImportDialogTitle -> "Импорт"
    ExportDialogTitle -> "Экспорт"
    SetResolutionDialogDescription -> "Введите разрешение холста (точек на пиксель) для редактора (%d ~ %d)"
    SetEntryPropertyDialogDescription ->
        "Пожалуйста, введите значение свойства `%s` этой записи. \n" +
            "Обратите внимание, что ваше входное значение не будет ограничено, " +
            "поэтому, пожалуйста, самостоятельно проверьте достоверность данных после этого процесса."
    AskIfSaveBeforeOpenDialogDescription ->
        "У вас есть несохраненные изменения. Хотите ли вы сохранить их перед открытием " +
            "нового проекта?"
    AskIfSaveBeforeExportDialogDescription -> "У вас есть несохраненные изменения. Хотите ли вы сохранить их перед экспортом?"
    AskIfSaveBeforeCloseDialogDescription ->
        "У вас есть несохраненные изменения. Хотите ли вы сохранить их перед закрытием " +
            "текущего проекта?"
    AskIfSaveBeforeExitDialogDescription -> "У вас есть несохраненные изменения. Хотите ли вы сохранить их перед выходом?"
    InputEntryNameDialogDescription -> "Переименовать запись"
    InputEntryNameDuplicateDialogDescription -> "Введите имя для новой записи"
    InputEntryNameCutFormerDialogDescription -> "Введите имя для первой записи после разрезания"
    InputEntryNameCutLatterDialogDescription -> "Введите имя для второй записи после разрезания"
    EditEntryNameDialogExistingError -> "Введенное вами имя уже существует."
    EditEntryExtraDialogDescription -> "Изменить дополнительную информацию текущей записи"
    EditModuleExtraDialogDescription -> "Изменить дополнительную информацию текущего подпроекта"
    MoveEntryDialogDescription -> "Введите новый индекс для записи \"%1\$s\" (%2\$d ~ %3\$d)"
    AskIfRemoveEntryDialogDescription -> "Удаление записи \"%s\"..."
    AskIfRemoveEntryLastDialogDescription ->
        "Удаление записи \"%s\"...\n" +
            "Эта запись — единственная, которая ссылается на текущий файл сэмпла.\n" +
            "Если вам понадобится добавить на него запись позже, смотрите меню `Вид` -> `Список сэмплов`."
    AskIfLoadAutoSavedProjectDialogDescription ->
        "Обнаружен файл автосохраненного проекта. Хотите загрузить его? " +
            "Файл будет отброшен, если вы откроете или создадите другой проект."
    AskIfRedirectSampleDirectoryDialogDescription ->
        "Каталог сэмплов текущего подпроекта (%s) не найден, " +
            "или не содержит ни одного требуемого файла сэмпла." +
            "Хотите переназначить его на новый каталог?"
    AskIfLabelFileChangeDetectedDialogDescription ->
        "Файл меток (label) был изменен внешней программой. " +
            "Хотите перезагрузить файл, чтобы отразить изменения?\n" +
            "Вы можете настроить это поведение в `Предпочтения` -> `Автоперезагрузка`."
    PluginDialogTitle -> "vLabeler - Плагин"
    PluginDialogInfoAuthor -> "автор: %s"
    PluginDialogInfoVersion -> "версия: %d"
    PluginDialogInfoContact -> "Связаться с автором"
    PluginDialogDescriptionMin -> "мин: %s"
    PluginDialogDescriptionMax -> "макс: %s"
    PluginDialogDescriptionMinMax -> "мин: %s, макс: %s"
    PluginDialogExecute -> "Выполнить"
    PluginDialogImportFromSavedParams -> "Загрузить сохраненные параметры по умолчанию"
    PluginDialogImportFromSlot -> "Загрузить из слота %1\$d: %2\$s"
    PluginDialogEmptySlotName -> "(пусто)"
    PluginDialogImportFromFile -> "Импортировать из файла"
    PluginDialogImportSuccess -> "Пресет успешно импортирован."
    PluginDialogImportFailure -> "Не удалось импортировать пресет. Возможно, он несовместим с текущим элементом."
    PluginDialogExportToSavedParams -> "Сохранить параметры как по умолчанию"
    PluginDialogExportToSlot -> "Сохранить параметры в слот %1\$d: %2\$s"
    PluginDialogExportToFile -> "Экспортировать параметры в файл"
    PluginDialogExportSuccess -> "Пресет успешно экспортирован."
    PluginDialogExportFailure -> "Не удалось экспортировать пресет."
    PluginEntrySelectorTextMatchTypeEquals -> "Равно"
    PluginEntrySelectorTextMatchTypeContains -> "Содержит"
    PluginEntrySelectorTextMatchTypeStartsWith -> "Начинается с"
    PluginEntrySelectorTextMatchTypeEndsWith -> "Заканчивается на"
    PluginEntrySelectorTextMatchTypeRegex -> "Регулярное выражение"
    PluginEntrySelectorNumberMatchTypeEquals -> "="
    PluginEntrySelectorNumberMatchTypeGreaterThan -> ">"
    PluginEntrySelectorNumberMatchTypeGreaterThanOrEquals -> ">="
    PluginEntrySelectorNumberMatchTypeLessThan -> "<"
    PluginEntrySelectorNumberMatchTypeLessThanOrEquals -> "<="
    PluginEntrySelectorPreservedSubjectSample -> "Имя сэмпла (без расширения)"
    PluginEntrySelectorPreservedSubjectName -> "Имя записи"
    PluginEntrySelectorPreservedSubjectTag -> "Тег"
    PluginEntrySelectorPreservedSubjectDone -> "Готово"
    PluginEntrySelectorPreservedSubjectStar -> "Избранное"
    PluginEntrySelectorPreservedSubjectScript -> "Выражение"
    PluginEntrySelectorComparerValue -> "Ввод"
    PluginEntrySelectorPreviewSummaryError -> "Неверный ввод"
    PluginEntrySelectorPreviewSummaryInitializing -> "Инициализация..."
    PluginEntrySelectorPreviewSummary -> "Выбрано %d/%d"
    PluginEntrySelectorPlaceholder -> "Нет фильтров, выбраны все записи."
    PluginEntrySelectorExpressionTitle -> "Выражение"
    PluginEntrySelectorExpressionDescription ->
        "Логическое выражение для объединения настроенных выше фильтров.\n" +
            "Доступные токены: `and`, `or`, `not`, `xor`, `(`, `)`, `#1`, `#2` и т. д."
    EditorSubTitleMultiple -> "редактирование %1\$d записей в сэмпле %2\$s"
    FailedToLoadSampleFileError -> "Не удалось загрузить файл сэмпла.\nОн может не существовать или иметь неподдерживаемый формат."
    PluginRuntimeUnexpectedException ->
        "Произошла непредвиденная ошибка во время выполнения плагина.\n" +
            "Пожалуйста, свяжитесь с автором, приложив журнал ошибок, для получения дополнительной информации."
    FailedToCreateProjectException ->
        "Не удалось создать проект." +
            "Пожалуйста, проверьте настройки разметчика/плагина, чтобы убедиться, что вы настроили его правильно.\n" +
            "Если проблема не исчезнет, пожалуйста, свяжитесь с автором разметчика/плагина, приложив журнал ошибок, " +
            "для получения дополнительной информации."
    InvalidCreatedProjectException ->
        "Созданный проект недействителен. " +
            "Пожалуйста, проверьте настройки разметчика/плагина, чтобы убедиться, что вы настроили его правильно.\n" +
            "Если проблема не исчезнет, пожалуйста, свяжитесь с автором разметчика/плагина, приложив журнал ошибок, " +
            "для получения дополнительной информации."
    FailedToCreateProjectNoSampleException ->
        "Не удалось создать проект, потому что в каталоге сэмплов, заданном в настройках каталогов, не найден ни один файл сэмпла."
    InvalidOpenedProjectException ->
        "Не удалось открыть проект, так как он содержит недействительные данные.\n" +
            "Пожалуйста, проверьте журнал ошибок для получения дополнительной информации."
    ProjectParseException ->
        "Не удалось открыть проект.\n" +
            "Возможно, он поврежден или создан несовместимой версией vLabeler.\n" +
            "Попробуйте создать новый проект и импортировать файл проекта через меню `Файл` -> `Импорт...`."
    ProjectImportException ->
        "Не удалось импортировать файл.\n" +
            "Возможно, это недействительный файл проекта vLabeler. Пожалуйста, проверьте журнал ошибок для получения дополнительной информации."
    ProjectUpdateOnSampleException ->
        "Не удалось обновить проект загруженным файлом сэмпла." +
            "\nПожалуйста, проверьте журнал ошибок для получения дополнительной информации."
    InvalidEditedProjectException -> "Недействительный отредактированный проект.\nПожалуйста, проверьте журнал ошибок для получения дополнительной информации."
    CustomizableItemLoadingException -> "Не удалось загрузить выбранный настраиваемый элемент."
    CustomizableItemRemovingException -> "Не удалось удалить выбранный настраиваемый элемент."
    PluginRuntimeExceptionTemplate -> "Ошибка выполнения плагина: %s"
    ProjectConstructorRuntimeExceptionTemplate -> "Ошибка выполнения разметчика при создании проекта: %s"
    QuickProjectBuilderRuntimeExceptionTemplate -> "Ошибка выполнения разметчика при создании проекта для быстрого редактирования: %s"
    PropertySetterRuntimeExceptionTemplate -> "Ошибка выполнения разметчика при установке свойства: %s"
    PropertySetterUnexpectedRuntimeException ->
        "Произошла непредвиденная ошибка при установке свойства.\n" +
            "Пожалуйста, свяжитесь с автором разметчика, приложив журнал ошибок, для получения дополнительной информации."
    VideoComponentInitializationException ->
        "Не удалось инициализировать видеокомпонент. Для использования этой функции необходимо установить VLC на ваше устройство. " +
            "Подробности см. в разделе `Интеграция видео` в README."
    VideoFileNotFoundExceptionTemplate ->
        "Видео не найдено по имени \"%s\" с расширением из списка %s."
    LabelerManagerTitle -> "Разметчики (Labelers)"
    LabelerManagerImportDialogTitle -> "Импортировать разметчик"
    TemplatePluginManagerTitle -> "Генераторы шаблонов"
    TemplatePluginManagerImportDialogTitle -> "Импортировать генератор шаблонов"
    MacroPluginManagerTitle -> "Плагины пакетного редактирования"
    MacroPluginManagerImportDialogTitle -> "Импортировать плагин пакетного редактирования"
    MacroPluginReportDialogTitle -> "Результат выполнения пакетного редактирования"
    MacroPluginReportDialogCopy -> "Копировать"
    CustomizableItemManagerRemoveItemConfirm ->
        "Вы уверены, что хотите удалить \"%s\"? " +
            "Это приведет к удалению файла(ов) с диска."
    CustomizableItemManagerOpenDirectory -> "Открыть каталог"
    CustomizableItemManagerReload -> "Перезагрузить список"
    CustomizableItemManagerLockedDescription -> "Этот элемент встроен и не может быть удален."
    PreferencesEditorImport -> "Импорт"
    PreferencesEditorImportDialogTitle -> "Импортировать предпочтения"
    PreferencesEditorImportSuccess -> "Предпочтения успешно импортированы."
    PreferencesEditorImportFailure -> "Не удалось импортировать выбранный файл предпочтений."
    PreferencesEditorExport -> "Экспорт"
    PreferencesEditorExportSuccess -> "Предпочтения успешно экспортированы."
    PreferencesEditorExportFailure -> "Не удалось экспортировать предпочтения в выбранный файл."
    PreferencesEditorExportDialogTitle -> "Экспортировать предпочтения"
    PreferencesEditorResetPage -> "Сбросить элементы на этой странице"
    PreferencesEditorResetAll -> "Сбросить все элементы"
    PreferencesCharts -> "Графики"
    PreferencesChartsDescription -> "Настройка графиков, отображаемых в редакторе."
    PreferencesChartsCanvas -> "Холст"
    PreferencesChartsCanvasDescription -> "Настройка общих параметров холста, на котором рисуются графики."
    PreferencesChartsCanvasResolution -> "Разрешение холста"
    PreferencesChartsCanvasResolutionDescription ->
        "Определяется как количество точек сэмпла, включенных в 1 пиксель.\n" +
            "Чем больше число, тем больший промежуток времени отображается на графиках."
    PreferencesChartsCanvasResolutionDefault -> "Разрешение по умолчанию"
    PreferencesChartsCanvasResolutionStep -> "Шаг"
    PreferencesChartsMaxDataChunkSize -> "Максимальный размер фрагмента данных"
    PreferencesChartsMaxDataChunkSizeDescription ->
        "Максимальное количество кадров, которое будет включено во фрагмент графика.\n" +
            "Чем больше число, тем на меньшее количество частей будут разбиты ваши графики во время рендеринга."
    PreferencesChartsWaveform -> "Осциллограмма"
    PreferencesChartsWaveformDescription -> "Настройка графика осциллограммы."
    PreferencesChartsWaveformResampleDownTo -> "Максимальная частота дискретизации (Гц)"
    PreferencesChartsWaveformResampleDownToDescription ->
        "Аудиофайл с более высокой частотой дискретизации " +
            "будет передискретизирован до указанного значения (файлы не редактируются).\n" +
            "Установите 0 для отключения передискретизации."
    PreferencesChartsWaveformNormalize -> "Нормализовать аудио"
    PreferencesChartsWaveformNormalizeDescription ->
        "Нормализация занимает дополнительное время при первой загрузке сэмплов.\n" +
            "Аудиофайлы не редактируются во время нормализации."
    PreferencesChartsWaveformUnitSize -> "Точек на пиксель"
    PreferencesChartsWaveformUnitSizeDescription -> "Увеличьте для более низкого качества изображения."
    PreferencesChartsWaveformIntensityAccuracy -> "Высота битмапа (px)"
    PreferencesChartsWaveformYAxisBlankRate -> "Вертикальный отступ (%%)"
    PreferencesChartsWaveformColor -> "Цвет"
    PreferencesChartsWaveformBackgroundColor -> "Цвет фона"
    PreferencesChartsSpectrogram -> "Спектрограмма"
    PreferencesChartsSpectrogramDescription -> "Настройка графика спектрограммы."
    PreferencesChartsSpectrogramEnabled -> "Показать спектрограмму"
    PreferencesChartsSpectrogramHeight -> "Высота относительно осциллограмм (%%)"
    PreferencesChartsSpectrogramPointDensity -> "Точек на пиксель"
    PreferencesChartsSpectrogramPointDensityDescription -> "Увеличьте для более низкого качества изображения."
    PreferencesChartsSpectrogramHopSize -> "Размер шага БПФ"
    PreferencesChartsSpectrogramHopSizeDescription -> "Адаптируется к фактической частоте дискретизации."
    PreferencesChartsSpectrogramWindowSize -> "Размер окна"
    PreferencesChartsSpectrogramWindowSizeDescription -> "Адаптируется к фактической частоте дискретизации."
    PreferencesChartsSpectrogramMelScaleStep -> "Частотное разрешение (мел)"
    PreferencesChartsSpectrogramMaxFrequency -> "Максимальная отображаемая частота (Гц)"
    PreferencesChartsSpectrogramMinIntensity -> "Минимальная отображаемая интенсивность (дБ)"
    PreferencesChartsSpectrogramMinIntensityInvalid -> "Минимальная интенсивность должна быть меньше максимальной интенсивности."
    PreferencesChartsSpectrogramMaxIntensity -> "Максимальная отображаемая интенсивность (дБ)"
    PreferencesChartsSpectrogramMaxIntensityInvalid -> "Максимальная интенсивность должна быть больше минимальной интенсивности."
    PreferencesChartsSpectrogramWindowType -> "Оконная функция"
    PreferencesChartsSpectrogramColorPalette -> "Цвета"
    PreferencesChartsSpectrogramColorPaletteDescription ->
        "Цветовая палитра для спектрограммы. Вы можете добавить свои собственные цветовые палитры в @edit{эту папку}. " +
            "Вам нужно повторно открыть диалог настроек для перезагрузки изменений."
    PreferencesChartsSpectrogramUseHighAlphaContrast -> "Использовать высокий альфа-контраст"
    PreferencesChartsSpectrogramUseHighAlphaContrastDescription ->
        "Эта опция влияет только на цветовые палитры, имеющие значения альфа-канала."
    PreferencesChartsPower -> "Мощность"
    PreferencesChartsPowerDescription -> "Настройка графика мощности."
    PreferencesChartsPowerEnabled -> "Показать график мощности"
    PreferencesChartsPowerMergeChannels -> "Объединить каналы"
    PreferencesChartsPowerHeight -> "Высота относительно осциллограмм (%%)"
    PreferencesChartsPowerUnitSize -> "Точек на пиксель"
    PreferencesChartsPowerUnitSizeDescription -> "Увеличьте для более низкого качества изображения."
    PreferencesChartsPowerUnitSizeInvalid -> "Размер единицы должен быть меньше или равен размеру окна."
    PreferencesChartsPowerWindowSize -> "Размер окна"
    PreferencesChartsPowerWindowSizeInvalid -> "Размер окна должен быть больше или равен размеру единицы."
    PreferencesChartsPowerMinPower -> "Минимальная отображаемая мощность (дБ)"
    PreferencesChartsPowerMinPowerInvalid -> "Минимальная мощность должна быть меньше максимальной мощности."
    PreferencesChartsPowerMaxPower -> "Максимальная отображаемая мощность (дБ)"
    PreferencesChartsPowerMaxPowerInvalid -> "Максимальная мощность должна быть больше минимальной мощности."
    PreferencesChartsPowerIntensityAccuracy -> "Высота битмапа (px)"
    PreferencesChartsPowerColor -> "Цвет"
    PreferencesChartsPowerBackgroundColor -> "Цвет фона"
    PreferencesChartsFundamental -> "F0"
    PreferencesChartsFundamentalDescription -> "Настройка графика F0."
    PreferencesChartsFundamentalEnabled -> "Показать график F0"
    PreferencesChartsFundamentalHeight -> "Высота относительно осциллограмм (%%)"
    PreferencesChartsFundamentalSemitoneResolution -> "Разрешение полутонов на изображении"
    PreferencesChartsFundamentalMinFundamental -> "Минимальная отображаемая частота (Гц)"
    PreferencesChartsFundamentalMinFundamentalInvalid -> "Минимальная частота должна быть меньше максимальной частоты."
    PreferencesChartsFundamentalMaxFundamental -> "Максимальная отображаемая частота (Гц)"
    PreferencesChartsFundamentalMaxFundamentalInvalid -> "Максимальная частота должна быть больше минимальной частоты."
    PreferencesChartsFundamentalSemitoneSampleNum -> "Количество сэмплов для каждого полутона"
    PreferencesChartsFundamentalMaxHarmonicFrequency -> "Максимальная гармоническая частота (Гц)"
    PreferencesChartsFundamentalMaxHarmonicFrequencyInvalid ->
        "Максимальная гармоническая частота должна быть больше максимальной частоты."
    PreferencesChartsFundamentalDrawReferenceLine -> "Нарисовать опорные линии на нотах До (C)"
    PreferencesChartsFundamentalColor -> "Цвет"
    PreferencesChartsFundamentalReferenceLineColor -> "Цвет опорной линии"
    PreferencesChartsFundamentalBackgroundColor -> "Цвет фона"
    PreferencesChartsConversion -> "Поддержка аудиоформатов"
    PreferencesChartsConversionDescription -> "Настройка поддержки аудиоформатов, отличных от wav."
    PreferencesChartsConversionFFmpegPath -> "Путь к исполняемому файлу FFmpeg"
    PreferencesChartsConversionFFmpegPathDescription ->
        "Пожалуйста, установите @open{FFmpeg} и укажите путь к исполняемому файлу, " +
            "чтобы выполнять преобразование формата перед загрузкой. " +
            "Вам также может потребоваться убедиться, что ваша ОС разрешает выполнение этого файла перед его использованием в vLabeler. " +
            "Возможные ошибки могут быть связаны с неподписанными исполняемыми файлами или правами доступа."
    PreferencesChartsConversionFFmpegArgs -> "Аргументы FFmpeg, кроме ввода и вывода"
    PreferencesChartsConversionFFmpegUseForWav -> "Использовать преобразование FFmpeg для файлов wav"
    PreferencesKeymap -> "Назначение клавиш"
    PreferencesKeymapDescription -> "Настройка сочетаний клавиш для действий с клавиатурой/мышью."
    PreferencesKeymapKeyAction -> "Действия клавиш"
    PreferencesKeymapKeyActionDescription -> "Настройка сочетаний клавиш для действий."
    PreferencesKeymapMouseClickAction -> "Действия по щелчку мыши"
    PreferencesKeymapMouseClickActionDescription ->
        "Настройка сочетаний клавиш для действий по щелчку мыши.\n" +
            "Действие выполняется только при нажатии всех клавиш в сочетании."
    PreferencesKeymapMouseScrollAction -> "Действия по прокрутке мыши"
    PreferencesKeymapMouseScrollActionDescription ->
        "Настройка сочетаний клавиш для действий по прокрутке мыши.\n" +
            "Действие выполняется только при нажатии всех клавиш в сочетании."
    PreferencesKeymapEditDialogTitle -> "Редактирование привязки клавиш для:"
    PreferencesKeymapEditDialogDescriptionMouseClick ->
        "Нажмите левой/правой кнопкой мыши на текстовом поле с нажатыми другими " +
            "клавишами для ввода сочетания."
    PreferencesKeymapEditDialogDescriptionMouseScroll ->
        "Прокрутите колесо мыши на текстовом поле с нажатыми другими " +
            "клавишами для ввода сочетания."
    PreferencesKeymapEditDialogConflictingLabel -> "Уже назначено на:"
    PreferencesKeymapEditDialogConflictingWarning ->
        "Это сочетание клавиш уже назначено другим действиям.\n" +
            "Вы хотите удалить другие назначения?"
    PreferencesKeymapEditDialogConflictingWarningKeep -> "Оставить"
    PreferencesKeymapEditDialogConflictingWarningRemove -> "Удалить"
    PreferencesView -> "Вид"
    PreferencesViewDescription -> "Настройка внешнего вида"
    PreferencesViewLanguage -> "Язык"
    PreferencesViewFontFamily -> "Шрифт"
    PreferencesViewFontFamilyDescription ->
        "Шрифт (семейство), используемый в приложении.\n" +
            "Помимо существующих вариантов, вы можете добавить шрифты в @edit{эту папку}. " +
            "В настоящее время шрифты ttc поддерживаются не полностью. " +
            "Чтобы использовать все стили шрифта ttc, преобразуйте его в файлы ttf/otf заранее.\n" +
            "Вам нужно повторно открыть диалог настроек для перезагрузки добавленных шрифтов."
    PreferencesViewHideSampleExtension -> "Скрыть расширение файла сэмпла"
    PreferencesViewAppAccentColor -> "Акцентный цвет (светлый) приложения"
    PreferencesViewAppAccentColorVariant -> "Акцентный цвет (темный) приложения"
    PreferencesViewPinnedEntryListPosition -> "Положение закрепленного списка записей"
    PreferencesViewPositionLeft -> "Слева"
    PreferencesViewPositionRight -> "Справа"
    PreferencesViewPositionTop -> "Сверху"
    PreferencesViewPositionBottom -> "Снизу"
    PreferencesViewCornerPositionTopLeft -> "Вверху слева"
    PreferencesViewCornerPositionTopRight -> "Вверху справа"
    PreferencesViewCornerPositionCenterLeft -> "По центру слева"
    PreferencesViewCornerPositionCenterRight -> "По центру справа"
    PreferencesViewCornerPositionBottomLeft -> "Внизу слева"
    PreferencesViewCornerPositionBottomRight -> "Внизу справа"
    PreferencesFontSizeSmall -> "Маленький"
    PreferencesFontSizeMedium -> "Средний"
    PreferencesFontSizeLarge -> "Большой"
    PreferencesFontSizeExtraLarge -> "Очень большой"
    PreferencesEditor -> "Редактор"
    PreferencesEditorDescription -> "Настройка внешнего вида и поведения редактора."
    PreferencesEditorPlayerCursorColor -> "Цвет курсора проигрывателя"
    PreferencesEditorLockedDrag -> "Фиксированное перетаскивание"
    PreferencesEditorLockedDragDescription ->
        "Выберите условие для включения фиксированного перетаскивания при перемещении " +
            "линий параметров.\n" +
            "Когда эта функция включена, другие линии параметров будут перемещаться соответственно, чтобы " +
            "сохранить относительное положение к перемещаемому параметру."
    PreferencesEditorLockedDragUseLabeler -> "Использовать настройки, определенные разметчиком"
    PreferencesEditorLockedDragUseStart -> "Фиксированное перетаскивание по началу записи"
    PreferencesEditorLockedDragNever -> "Никогда не использовать фиксированное перетаскивание"
    PreferencesEditorLockedSettingParameterWithCursor -> "Применить фиксированное перетаскивание при установке курсором"
    PreferencesEditorLockedSettingParameterWithCursorDescription ->
        "Применять вышеуказанные настройки фиксированного перетаскивания также при " +
            "установке параметров с помощью действий клавиш «Установить параметр в позицию курсора»"
    PreferencesEditorNotes -> "Заметки"
    PreferencesEditorNotesDescription ->
        "Настройка внешнего вида и поведения редактора в отношении заметок " +
            "записи (избранное, готово, тег, дополнительная информация)."
    PreferencesEditorShowDone -> "Отображать статус «Готово»"
    PreferencesEditorShowStarred -> "Отображать статус «Избранное»"
    PreferencesEditorShowTag -> "Отображать теги"
    PreferencesEditorShowExtra -> "Отображать «Редактировать дополнительную информацию»"
    PreferencesEditorShowExtraDescription ->
        "Даже если включено, если разметчик не определяет дополнительную информацию, кнопка не будет показана."
    PreferencesEditorPostEditAction -> "Действия после редактирования"
    PreferencesEditorPostEditActionDescription -> "Определите действия, которые будут выполнены после редактирования записи."
    PreferencesEditorPostEditActionDone -> "Установить отредактированные записи как «Готово» после редактирования"
    PreferencesEditorPostEditActionNext -> "Перейти к следующей записи после редактирования"
    PreferencesEditorPostEditActionEnabled -> "Включено"
    PreferencesEditorPostEditActionTrigger -> "Параметр(ы) для запуска действия"
    PreferencesEditorPostEditActionTriggerUseLabeler -> "Использовать настройки, определенные разметчиком"
    PreferencesEditorPostEditActionTriggerUseStart -> "Начало записи"
    PreferencesEditorPostEditActionTriggerUseEnd -> "Конец записи"
    PreferencesEditorPostEditActionTriggerUseAny -> "Любой параметр"
    PreferencesEditorPostEditActionUseDragging -> "Запуск при перетаскивании"
    PreferencesEditorPostEditActionUseDraggingDescription -> "Запустить действие после перетаскивания линии(ий) параметра."
    PreferencesEditorPostEditActionUseCursorSet -> "Запуск при «Установить параметр в позицию курсора»"
    PreferencesEditorPostEditActionUseCursorSetDescription ->
        "Запустить действие после установки параметра(ов) с помощью действий клавиш «Установить параметр в позицию курсора»."
    PreferencesEditorScissors -> "Ножницы"
    PreferencesEditorScissorsDescription -> "Настройка внешнего вида и поведения инструмента «Ножницы»."
    PreferencesEditorScissorsUseOnScreenScissors -> "Вводить имена записей прямо в редакторе"
    PreferencesEditorScissorsUseOnScreenScissorsDescription ->
        "Если включено, поле ввода имени новой записи будет отображаться в редакторе после нажатия " +
            "ножницами. Вы можете подтвердить действие разрезания, нажав Enter или отдалив курсор " +
            "от места нажатия. Вы также можете отменить действие, нажав Esc. Доступно только " +
            "в режиме множественного редактирования."
    PreferencesEditorScissorsScissorsSubmitThreshold -> "Порог подтверждения действия (DP)"
    PreferencesEditorScissorsScissorsSubmitThresholdDescription ->
        "После нажатия ножницами, когда вы отдаляете курсор от места нажатия и расстояние " +
            "превышает порог, действие разрезания будет подтверждено."
    PreferencesEditorScissorsColor -> "Цвет"
    PreferencesEditorScissorsActionTargetNone -> "Нет"
    PreferencesEditorScissorsActionTargetFormer -> "Первая запись"
    PreferencesEditorScissorsActionTargetLatter -> "Вторая запись"
    PreferencesEditorScissorsActionGoTo -> "Перейти к записи после разрезания"
    PreferencesEditorScissorsActionAskForName -> "Переименовать запись после разрезания"
    PreferencesEditorScissorsActionPlay -> "Воспроизвести аудио при разрезании"
    PreferencesEditorAutoScroll -> "Автопрокрутка"
    PreferencesEditorAutoScrollDescription ->
        "Определите, когда редактор будет автоматически прокручиваться, чтобы показать " +
            "текущую запись."
    PreferencesEditorAutoScrollOnLoadedNewSample -> "При переключении на другой сэмпл"
    PreferencesEditorAutoScrollOnJumpedToEntry -> "При переходе к другой записи по абсолютному индексу"
    PreferencesEditorAutoScrollOnSwitchedInMultipleEditMode -> "При переключении на другую запись в режиме множественного редактирования"
    PreferencesEditorAutoScrollOnSwitched -> "При переключении на другую запись"
    PreferencesEditorContinuousLabelNames -> "Имена меток (непрерывные)"
    PreferencesEditorContinuousLabelNamesDescription ->
        "Настройка внешнего вида текста имен записей, отображаемого в редакторе, при использовании непрерывного разметчика."
    PreferencesEditorContinuousLabelNamesColor -> "Цвет"
    PreferencesEditorContinuousLabelNamesBackgroundColor -> "Цвет фона"
    PreferencesEditorContinuousLabelNamesEditableBackgroundColor -> "Цвет фона (редактирование)"
    PreferencesEditorContinuousLabelNamesSize -> "Размер"
    PreferencesEditorContinuousLabelNamesPosition -> "Позиция"
    PreferencesEditorBorderHighlight -> "Выделение границ"
    PreferencesEditorBorderHighlightDescription -> "Настройка выделения границ в режиме множественного редактирования."
    PreferencesEditorHighlightCurrentEntryBorder -> "Выделить границы текущей записи"
    PreferencesEditorHighlightCurrentEntryBorderDescription ->
        "В режиме множественного редактирования выделить границы записи, которая в данный момент выбрана в списке записей."
    PreferencesEditorHighlightCursorPositionEntryBorder -> "Выделить границы записи по позиции курсора"
    PreferencesEditorHighlightCursorPositionEntryBorderDescription ->
        "В режиме множественного редактирования выделить границы записи, на которой в данный момент расположен курсор."
    PreferencesEditorHighlightEntryBorderEnabled -> "Включено"
    PreferencesEditorHighlightEntryBorderColor -> "Цвет"
    PreferencesEditorHighlightEntryBorderWidth -> "Ширина"
    PreferencesPlayback -> "Воспроизведение"
    PreferencesPlaybackDescription -> "Настройка поведения воспроизведения аудио."
    PreferencesPlaybackPlayOnDragging -> "Предварительный просмотр при перетаскивании"
    PreferencesPlaybackPlayOnDraggingDescription ->
        "При перетаскивании линий параметров с нажатыми клавишами для «Предварительный просмотр при перетаскивании» (см. назначение клавиш), " +
            "воспроизводить диапазон аудио рядом с курсором."
    PreferencesPlaybackPlayOnDraggingEnabled -> "Включено"
    PreferencesPlaybackPlayOnDraggingRangeRadiusMillis -> "Радиус (мс)"
    PreferencesPlaybackPlayOnDraggingEventQueueSize -> "Сохранять события перетаскивания"
    PreferencesAutoSave -> "Автосохранение"
    PreferencesAutoSaveDescription -> "Настройка поведения автосохранения проекта."
    PreferencesAutoSaveTarget -> "Расположение автосохраненного файла"
    PreferencesAutoSaveTargetNone -> "Не выполнять автосохранение"
    PreferencesAutoSaveTargetProject -> "Перезаписать файл проекта"
    PreferencesAutoSaveTargetRecord -> "Сохранить во временный файл"
    PreferencesAutoSaveIntervalSec -> "Интервал (сек)"
    PreferencesAutoReload -> "Автоперезагрузка"
    PreferencesAutoReloadDescription -> "Настройка поведения автоперезагрузки файлов меток (label), когда они изменяются."
    PreferencesAutoReloadBehavior -> "Поведение"
    PreferencesAutoReloadBehaviorDisabled -> "Отключено"
    PreferencesAutoReloadBehaviorAskWithDetails -> "Спросить с подробностями"
    PreferencesAutoReloadBehaviorAsk -> "Спросить Да/Нет"
    PreferencesAutoReloadBehaviorAuto -> "Автоматически"
    PreferencesHistory -> "История редактирования"
    PreferencesHistoryDescription -> "Настройка поведения истории редактирования (отмена/повтор)."
    PreferencesHistoryMaxSize -> "Максимальный сохраняемый размер"
    PreferencesHistorySquashIndex -> "Сжимать изменения индекса"
    PreferencesHistorySquashIndexDescription ->
        "Если включено, изменения индекса (например, переключение записей) не будут сохраняться " +
            "до следующего изменения содержимого."
    PreferencesMisc -> "Разное"
    PreferencesMiscDescription -> "Настройка другого поведения. Обратите внимание, что эти настройки могут быть экспериментальными."
    PreferencesMiscUpdateChannel -> "Канал обновлений"
    PreferencesMiscUpdateChannelDescription ->
        "Канал обновлений, используемый при автоматической проверке обновлений или в меню `Справка` -> `Проверить наличие обновлений...`."
    UpdateChannelStable -> "Стабильный"
    UpdateChannelPreview -> "Предварительный"
    PreferencesMiscUseCustomFileDialog -> "Использовать пользовательские диалоги файлов вместо системных"
    PreferencesMiscUseCustomFileDialogDescription ->
        "Если включено, диалоги файлов будут заменены пользовательскими. " +
            "Это может быть полезно, если вы используете среду рабочего стола, которая не поддерживает " +
            "системные диалоги файлов. Возможно, потребуется перезапустить приложение, чтобы применить эту настройку."
    PreferencesMiscDangerZone -> "Опасная зона"
    PreferencesMiscClearRecord -> "Очистить историю использования приложения"
    PreferencesMiscClearRecordDescription ->
        "Удалить все данные об использовании приложения, не управляемые предпочтениями, " +
            "такие как размеры окон, пропущенные версии обновлений, сохраненные настройки слотов плагинов и т. д."
    PreferencesMiscClearRecordButton -> "Очистить"
    PreferencesMiscClearRecordConfirmation ->
        "Вы уверены, что хотите очистить все данные истории использования приложения?\n" +
            "Это действие нельзя отменить.\n" +
            "Приложение немедленно закроется после очистки, без сохранения открытого в данный момент проекта."
    PreferencesMiscClearAppData -> "Стереть все данные приложения"
    PreferencesMiscClearAppDataDescription ->
        "Удалить все данные приложения, включая предпочтения, историю использования, установленные плагины и т. д."
    PreferencesMiscClearAppDataButton -> "Стереть все данные"
    PreferencesMiscClearAppDataConfirmation ->
        "Вы уверены, что хотите стереть все данные приложения?\n" +
            "Это действие нельзя отменить, и оно удалит все данные, связанные с vLabeler, с вашего устройства, " +
            "за исключением файлов, которые вы явно создали, например, файлов, связанных с проектом.\n" +
            "Приложение немедленно закроется после стирания, без сохранения открытого в данный момент проекта."
    ActionToggleSamplePlayback -> "Переключить воспроизведение текущего сэмпла"
    ActionToggleEntryPlayback -> "Переключить воспроизведение текущей записи"
    ActionToggleScreenRangePlayback -> "Переключить воспроизведение текущего диапазона экрана"
    ActionToggleVideoPopupEmbedded -> "Переключить отображение видео (встроенное)"
    ActionToggleVideoPopupNewWindow -> "Переключить отображение видео (в новом окне)"
    ActionIncreaseResolution -> "Уменьшить масштаб"
    ActionDecreaseResolution -> "Увеличить масштаб"
    ActionInputResolution -> "Ввести разрешение холста"
    ActionCancelDialog -> "Закрыть диалог"
    ActionScissorsCut -> "Разрезать в текущей позиции курсора"
    ActionSetValue1 -> "Установить Параметр 1 в позицию курсора"
    ActionSetValue2 -> "Установить Параметр 2 в позицию курсора"
    ActionSetValue3 -> "Установить Параметр 3 в позицию курсора"
    ActionSetValue4 -> "Установить Параметр 4 в позицию курсора"
    ActionSetValue5 -> "Установить Параметр 5 в позицию курсора"
    ActionSetValue6 -> "Установить Параметр 6 в позицию курсора"
    ActionSetValue7 -> "Установить Параметр 7 в позицию курсора"
    ActionSetValue8 -> "Установить Параметр 8 в позицию курсора"
    ActionSetValue9 -> "Установить Параметр 9 в позицию курсора"
    ActionSetValue10 -> "Установить Параметр 10 в позицию курсора"
    ActionSetProperty1 -> "Ввести значение для Свойства 1"
    ActionSetProperty2 -> "Ввести значение для Свойства 2"
    ActionSetProperty3 -> "Ввести значение для Свойства 3"
    ActionSetProperty4 -> "Ввести значение для Свойства 4"
    ActionSetProperty5 -> "Ввести значение для Свойства 5"
    ActionSetProperty6 -> "Ввести значение для Свойства 6"
    ActionSetProperty7 -> "Ввести значение для Свойства 7"
    ActionSetProperty8 -> "Ввести значение для Свойства 8"
    ActionSetProperty9 -> "Ввести значение для Свойства 9"
    ActionSetProperty10 -> "Ввести значение для Свойства 10"
    ActionQuickLaunch1 -> "Запустить плагин Слот 1"
    ActionQuickLaunch2 -> "Запустить плагин Слот 2"
    ActionQuickLaunch3 -> "Запустить плагин Слот 3"
    ActionQuickLaunch4 -> "Запустить плагин Слот 4"
    ActionQuickLaunch5 -> "Запустить плагин Слот 5"
    ActionQuickLaunch6 -> "Запустить плагин Слот 6"
    ActionQuickLaunch7 -> "Запустить плагин Слот 7"
    ActionQuickLaunch8 -> "Запустить плагин Слот 8"
    ActionMoveParameter -> "Перетащить линию параметра"
    ActionMoveParameterWithPlaybackPreview -> "Перетащить линию параметра с предварительным просмотром воспроизведения"
    ActionMoveParameterIgnoringConstraints -> "Перетащить линию параметра, игнорируя ограничения"
    ActionMoveParameterInvertingLocked -> "Перетащить линию параметра с инвертированными настройками фиксированного перетаскивания"
    ActionPlayAudioSection -> "Воспроизвести нажатую часть аудио"
    ActionPlayAudioUntilEnd -> "Воспроизвести аудио от нажатой позиции до конца"
    ActionPlayAudioUntilScreenEnd -> "Воспроизвести аудио от нажатой позиции до конца экрана"
    ActionPlayAudioFromStart -> "Воспроизвести аудио от начала до нажатой позиции"
    ActionPlayAudioFromScreenStart -> "Воспроизвести аудио от начала экрана до нажатой позиции"
    ActionPlayAudioRange -> "Воспроизвести аудио в перетаскиваемом диапазоне"
    ActionPlayAudioRangeRepeat -> "Повторно воспроизвести аудио в перетаскиваемом диапазоне"
    ActionScrollCanvasLeft -> "Прокрутить холст влево"
    ActionScrollCanvasRight -> "Прокрутить холст вправо"
    ActionZoomInCanvas -> "Увеличить масштаб"
    ActionZoomOutCanvas -> "Уменьшить масштаб"
    ActionGoToNextEntry -> "Перейти к следующей записи"
    ActionGoToPreviousEntry -> "Перейти к предыдущей записи"
    ActionGoToNextSample -> "Перейти к следующему сэмплу"
    ActionGoToPreviousSample -> "Перейти к предыдущему сэмплу"
    CheckForUpdatesAlreadyUpdated -> "У вас уже установлена последняя версия vLabeler."
    CheckForUpdatesFailure -> "Не удалось получить информацию о последней версии."
    UpdaterDialogSummaryDetailsLink -> "Подробности"
    UpdaterDialogTitle -> "vLabeler - Обновление"
    UpdaterDialogCurrentVersionLabel -> "Текущая версия: %s"
    UpdaterDialogLatestVersionLabel -> "Последняя версия: %1\$s (%2\$s)"
    UpdaterDialogStartDownloadButton -> "Скачать"
    UpdaterDialogIgnoreButton -> "Игнорировать эту версию"
    UpdaterDialogDownloadPositionLabel -> "Расположение загрузки: "
    UpdaterDialogChangeDownloadPositionButton -> "Изменить"
    UpdaterDialogChooseDownloadPositionDialogTitle -> "Выбрать расположение загрузки"
    AboutDialogTitle -> "vLabeler - О программе"
    AboutDialogCopyInfo -> "Копировать информацию"
    AboutDialogShowLicenses -> "Показать лицензии"
    LicenseDialogTitle -> "vLabeler - Лицензии"
    LicenseDialogLicenses -> "Лицензии OSS, используемые в vLabeler"
    LoadProjectErrorLabelerNotFound ->
        "Не удается найти необходимый разметчик `%1\$s` (версия %2\$s) на этом устройстве. " +
            "Пожалуйста, установите его вручную перед открытием проекта."
    LoadProjectWarningLabelerCreated -> "Новый разметчик `%s` установлен из файла проекта."
    LoadProjectWarningLabelerUpdated -> "Разметчик `%s` обновлен до версии `%s` из файла проекта."
    LoadProjectWarningCacheDirReset ->
        "Не удается найти или создать каталог кэша, определенный в файле проекта. " +
            "Вместо этого будет использоваться каталог кэша по умолчанию."
    FilterStarred -> "Фильтрация избранных записей"
    FilterUnstarred -> "Фильтрация не избранных записей"
    FilterStarIgnored -> "Не фильтруется по избранному"
    FilterDone -> "Фильтрация завершенных записей"
    FilterUndone -> "Фильтрация незавершенных записей"
    FilterDoneIgnored -> "Не фильтруется по завершенным"
    FilterLink -> "Переключить, чтобы применить фильтры к навигации по проекту"
    FilterLinked -> "Фильтры применены к навигации по проекту"
    FilterAdvancedInUse -> "Расширенные фильтры"
    FilterDisabledDueToAdvancedInUse -> "Отключено, так как применены расширенные фильтры"
    ColorPickerDialogTitle -> "vLabeler - Выбор цвета"
    QuickLaunchManagerDialogTitle -> "Слоты плагинов"
    QuickLaunchManagerDialogDescription ->
        "Вы можете назначить часто используемые плагины пакетного редактирования с их параметрами слотам плагинов " +
            "для быстрого доступа. Параметры, хранящиеся в слотах плагинов, независимы друг от друга, " +
            "и не влияют на параметры, сохраненные при обычном использовании."
    QuickLaunchManagerDialogHeaderTitle -> "Слот"
    QuickLaunchManagerDialogHeaderPlugin -> "Плагин"
    QuickLaunchManagerDialogHeaderForceAskParams -> "Всегда запрашивать параметры"
    QuickLaunchManagerDialogItemTitle -> "Слот %d"
    QuickLaunchManagerDialogOpenKeymap -> "Открыть назначение клавиш"
    TrackingSettingsDialogTitle -> "Отслеживать использование приложения"
    TrackingSettingsDialogDescription ->
        "Здесь вы можете включить/отключить отслеживание данных об использовании, которое собирает анонимные события использования, такие как " +
            "`Запуск приложения` и `Использование плагина`. Собранные данные используются для улучшения vLabeler, " +
            "и они не включают никакой конкретной информации о вашем проекте, данных или личной информации. " +
            "Пожалуйста, включите отслеживание, если считаете это приемлемым. " +
            "Вы можете узнать больше подробностей, нажав кнопку `Подробности`."
    TrackingSettingsDialogFirstTimeAlert ->
        "Мы показываем вам этот диалог, потому что вы используете vLabeler с отслеживанием в первый раз. " +
            "Вы можете открыть этот диалог и изменить настройки отслеживания в любое время в меню " +
            "`Настройки` -> `Отслеживать использование приложения...`"
    TrackingSettingsDialogEnabled -> "Включено"
    TrackingSettingsDialogTrackingIdLabel -> "ID отслеживания:"
    ProjectSettingDialogTitle -> "Настройки проекта"
    ProjectSettingOutputFileLabel -> "Выходной файл"
    ProjectSettingOutputFileHelperText ->
        "Целевой файл для действия `Экспорт`.\nЕсли не задан, `Экспорт с заменой` отключен."
    ProjectSettingOutputFileDisabledPlaceholder -> "Отключено текущим разметчиком"
    ProjectSettingOutputFileSelectorDialogTitle -> "Выбрать выходной файл"
    ProjectSettingAutoExportHelperText ->
        "Автоматически экспортировать все подпроекты в их выходные файлы при сохранении проекта.\n" +
            "Эта опция действует только при корректной установке `Выходного файла`,\n" +
            "или если он зафиксирован разметчиком."
    ImportEntriesDialogTitle -> "Импортировать проект"
    ImportEntriesDialogItemSummaryTitle -> "%d записей"
    ImportEntriesDialogItemTargetLabel -> "Цель"
    ImportEntriesDialogItemIncompatible -> "Несовместим с текущим проектом"
    ImportEntriesDialogReplaceContent -> "Удалить текущие записи"
    ImportEntriesDialogReplaceContentDisabledDescription ->
        "Текущий проект не поддерживает добавление записей, " +
            "поэтому все текущие записи будут удалены перед импортом."
    EntrySampleSyncerModuleText -> "Обработка подпроектов %d/%d..."
    EntrySampleSyncerModuleTextFinished -> "Обработка подпроектов %d/%d... Готово"
    EntrySampleSyncerSampleText -> "Обработка файлов сэмплов %d/%d..."
    EntrySampleSyncerSampleTextFinished -> "Обработка файлов сэмплов %d/%d... Готово"
    FFmpegConverterException ->
        "Не удалось загрузить файл сэмпла. Чтобы загрузить форматы, отличные от wav, " +
            "пожалуйста, установите FFmpeg и настройте его путь в `Предпочтения` -> `Графики` -> `Поддержка аудиоформатов`. " +
            "Если вы уже установили и настроили FFmpeg, проверьте журнал для получения более подробной информации."
    AppRunningOnCompatibilityModeWarning ->
        "vLabeler работает в режиме совместимости Rosetta 2.\n" +
            "Для лучшей производительности попробуйте сборку для Apple Silicon (~mac-arm64.dmg)."
    EntryFilterSetterDialogTitle -> "Настройки фильтра"
    EntryFilterSetterDialogModeBasic -> "Основной"
    EntryFilterSetterDialogModeAdvanced -> "Расширенный"
    EntryFilterSetterDialogHeaderAny -> "Любой текст содержит"
    EntryFilterSetterDialogHeaderName -> "Имя записи содержит"
    EntryFilterSetterDialogHeaderSample -> "Имя сэмпла содержит"
    EntryFilterSetterDialogHeaderTag -> "Тег содержит"
    EntryFilterSetterDialogHeaderStar -> "Избранное"
    EntryFilterSetterDialogHeaderDone -> "Готово"
    FileNameNormalizerDialogTitle -> "vLabeler - Нормализатор имен файлов"
    FileNameNormalizerTitle -> "Нормализатор имен файлов"
    FileNameNormalizerDescription ->
        "Этот инструмент предназначен для преобразования имен файлов из кодировки NFD (обычно используется в macOS) в " +
            "NFC (обычно используется в Windows)." +
            "Выберите папку для рекурсивного преобразования всех имен файлов в ней, " +
            "или выберите файл для преобразования его содержимого.\n" +
            "Если вы уже создали проект на основе файлов сэмплов, убедитесь, что вы преобразовали как имена файлов сэмплов, " +
            "так и содержимое файла проекта."
    FileNameNormalizerHandleFolderButton -> "Выбрать папку"
    FileNameNormalizerHandleFileContentButton -> "Выбрать файл"
    FileNameNormalizerHandleFolderSuccess -> "Отсканировано %d файлов, преобразовано %d файлов."
    FileNameNormalizerHandleFileSuccess -> "Содержимое файла успешно преобразовано."
    FileNameNormalizerHandleFileNoChange -> "Для содержимого файла преобразование не требуется."
    ReloadLabelDialogTitle -> "Перезагрузить файл меток (Label)"
    ReloadLabelDialogModuleNameTemplate -> "Подпроект: %s"
    ReloadLabelDialogShowUnchanged -> "Показать без изменений"
    ReloadLabelDialogNotice ->
        "Этот предварительный просмотр различий рассчитывается на основе настроек текущего разметчика, " +
            "поэтому он может не показывать все мелкие различия. Пожалуйста, проверьте фактическое содержимое после перезагрузки, чтобы убедиться, что " +
            "все в порядке."
    ReloadLabelDialogNoDiff -> "Различий не найдено."
    ReloadLabelDialogInheritNotes -> "Унаследовать заметки"
    ReloadLabelDialogInheritNotesDescription ->
        "Если включено, заметки старых записей будут скопированы в новые записи для всех совпадающих пар записей."
    EditorContextActionOpenRenameEntryDialog -> "Переименовать запись..."
    EditorContextActionOpenDuplicateEntryDialog -> "Дублировать запись..."
    EditorContextActionOpenRemoveEntryDialog -> "Удалить запись..."
    EditorContextActionOpenMoveEntryDialog -> "Переместить запись в..."
    EditorContextActionCopyEntryName -> "Копировать имя записи в буфер обмена"
    EditorContextActionFilterByEntryName -> "Фильтровать по имени записи"
    EditorContextActionCopySampleName -> "Копировать имя сэмпла в буфер обмена"
    EditorContextActionFilterBySampleName -> "Фильтровать по имени сэмпла"
    EditorContextActionFilterByTag -> "Фильтровать по тегу"
    EditorContextActionFilterStarred -> "Фильтровать избранные записи"
    EditorContextActionFilterUnstarred -> "Фильтровать не избранные записи"
    EditorContextActionFilterDone -> "Фильтровать завершенные записи"
    EditorContextActionFilterUndone -> "Фильтровать незавершенные записи"
}
