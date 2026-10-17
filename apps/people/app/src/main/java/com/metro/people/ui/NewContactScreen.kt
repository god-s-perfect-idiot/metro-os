package com.metro.people.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.ContactAccountTarget
import com.metro.people.data.ContactDraft
import com.metro.people.data.ContactEditorLogic
import com.metro.people.data.ContactPrefill
import com.metro.people.data.TypedContactValue
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NewContactScreen(
    prefill: ContactPrefill?,
    accounts: List<ContactAccountTarget>,
    onSave: (draft: ContactDraft, onFinished: () -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(prefill) {
        mutableStateOf(ContactEditorLogic.applyPrefill(prefill))
    }
    var expanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(accounts, draft.account) {
        if (accounts.isEmpty()) return@LaunchedEffect
        val stillValid = accounts.any {
            it.accountName == draft.account.accountName &&
                it.accountType == draft.account.accountType
        }
        if (!stillValid) {
            draft = draft.copy(account = accounts.first())
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            draft = draft.copy(photoUri = uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = stringResource(R.string.new_contact).uppercase(),
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp, bottom = 20.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    // App bar + expanded account ListPicker (up to ~6 rows) need room below.
                    .padding(bottom = MetroAppBarDefaults.BarHeight + 280.dp),
            ) {
                PhotoPickerRow(
                    photoUri = draft.photoUri,
                    onAddPhoto = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onClearPhoto = { draft = draft.copy(photoUri = null) },
                )

                Spacer(modifier = Modifier.height(20.dp))

                FieldLabel(stringResource(R.string.field_name))
                MetroTextBox(
                    value = draft.givenName,
                    onValueChange = { draft = draft.copy(givenName = it) },
                    placeholder = stringResource(R.string.field_name),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))
                PhoneRows(
                    phones = draft.phones,
                    onChange = { draft = draft.copy(phones = it) },
                )

                Spacer(modifier = Modifier.height(16.dp))
                EmailRows(
                    emails = draft.emails,
                    onChange = { draft = draft.copy(emails = it) },
                )

                if (!expanded) {
                    Spacer(modifier = Modifier.height(20.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.show_more_fields),
                        onClick = { expanded = true },
                    )
                } else {
                    ExpandedFields(
                        draft = draft,
                        onDraftChange = { draft = it },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                MetroListPicker(
                    selected = draft.account,
                    options = accounts.map { MetroListPickerOption(it, it.label) },
                    onSelectedChange = { draft = draft.copy(account = it) },
                    label = stringResource(R.string.save_to_account),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MetroAppBar(
            visible = true,
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Save,
                    label = "save",
                    onClick = {
                        if (saving || !ContactEditorLogic.canSave(draft)) return@MetroAppBarIcon
                        saving = true
                        onSave(draft) { saving = false }
                    },
                    contentDescription = stringResource(R.string.save_contact),
                    enabled = !saving && ContactEditorLogic.canSave(draft),
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = "cancel",
                    onClick = onCancel,
                    contentDescription = stringResource(R.string.cancel),
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PhotoPickerRow(
    photoUri: Uri?,
    onAddPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(photoUri) {
        bitmap = if (photoUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(photoUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MetroTheme.colors.secondarySurface)
                .metroClickable(onClick = onAddPhoto),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = stringResource(R.string.contact_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                MetroText(
                    text = "+",
                    style = MetroTextStyle.PageTitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            MetroBorderButton(
                text = stringResource(R.string.add_photo),
                onClick = onAddPhoto,
            )
            if (photoUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                MetroBorderButton(
                    text = stringResource(R.string.remove_photo),
                    onClick = onClearPhoto,
                )
            }
        }
    }
}

@Composable
private fun ExpandedFields(
    draft: ContactDraft,
    onDraftChange: (ContactDraft) -> Unit,
) {
    Spacer(modifier = Modifier.height(20.dp))
    FieldLabel(stringResource(R.string.field_last_name))
    MetroTextBox(
        value = draft.familyName,
        onValueChange = { onDraftChange(draft.copy(familyName = it)) },
        placeholder = stringResource(R.string.field_last_name),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_middle_name))
    MetroTextBox(
        value = draft.middleName,
        onValueChange = { onDraftChange(draft.copy(middleName = it)) },
        placeholder = stringResource(R.string.field_middle_name),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_name_prefix))
    MetroTextBox(
        value = draft.namePrefix,
        onValueChange = { onDraftChange(draft.copy(namePrefix = it)) },
        placeholder = stringResource(R.string.field_name_prefix),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_name_suffix))
    MetroTextBox(
        value = draft.nameSuffix,
        onValueChange = { onDraftChange(draft.copy(nameSuffix = it)) },
        placeholder = stringResource(R.string.field_name_suffix),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_phonetic_name))
    MetroTextBox(
        value = draft.phoneticName,
        onValueChange = { onDraftChange(draft.copy(phoneticName = it)) },
        placeholder = stringResource(R.string.field_phonetic_name),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_nickname))
    MetroTextBox(
        value = draft.nickname,
        onValueChange = { onDraftChange(draft.copy(nickname = it)) },
        placeholder = stringResource(R.string.field_nickname),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_company))
    MetroTextBox(
        value = draft.company,
        onValueChange = { onDraftChange(draft.copy(company = it)) },
        placeholder = stringResource(R.string.field_company),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_job_title))
    MetroTextBox(
        value = draft.jobTitle,
        onValueChange = { onDraftChange(draft.copy(jobTitle = it)) },
        placeholder = stringResource(R.string.field_job_title),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_department))
    MetroTextBox(
        value = draft.department,
        onValueChange = { onDraftChange(draft.copy(department = it)) },
        placeholder = stringResource(R.string.field_department),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(16.dp))
    MetroListPicker(
        selected = draft.postalType,
        options = ContactEditorLogic.postalTypes.map {
            MetroListPickerOption(it.type, it.label)
        },
        onSelectedChange = { onDraftChange(draft.copy(postalType = it)) },
        label = stringResource(R.string.field_address_type),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_street))
    MetroTextBox(
        value = draft.postalStreet,
        onValueChange = { onDraftChange(draft.copy(postalStreet = it)) },
        placeholder = stringResource(R.string.field_street),
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_city))
    MetroTextBox(
        value = draft.postalCity,
        onValueChange = { onDraftChange(draft.copy(postalCity = it)) },
        placeholder = stringResource(R.string.field_city),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_region))
    MetroTextBox(
        value = draft.postalRegion,
        onValueChange = { onDraftChange(draft.copy(postalRegion = it)) },
        placeholder = stringResource(R.string.field_region),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_postcode))
    MetroTextBox(
        value = draft.postalPostcode,
        onValueChange = { onDraftChange(draft.copy(postalPostcode = it)) },
        placeholder = stringResource(R.string.field_postcode),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_country))
    MetroTextBox(
        value = draft.postalCountry,
        onValueChange = { onDraftChange(draft.copy(postalCountry = it)) },
        placeholder = stringResource(R.string.field_country),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_website))
    MetroTextBox(
        value = draft.website,
        onValueChange = { onDraftChange(draft.copy(website = it)) },
        placeholder = stringResource(R.string.field_website),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )

    Spacer(modifier = Modifier.height(12.dp))
    MetroListPicker(
        selected = draft.imProtocol,
        options = ContactEditorLogic.imProtocols.map {
            MetroListPickerOption(it.type, it.label)
        },
        onSelectedChange = { onDraftChange(draft.copy(imProtocol = it)) },
        label = stringResource(R.string.field_im_protocol),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_im))
    MetroTextBox(
        value = draft.imHandle,
        onValueChange = { onDraftChange(draft.copy(imHandle = it)) },
        placeholder = stringResource(R.string.field_im),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_birthday))
    MetroTextBox(
        value = draft.birthday,
        onValueChange = { onDraftChange(draft.copy(birthday = it)) },
        placeholder = stringResource(R.string.field_date_hint),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_anniversary))
    MetroTextBox(
        value = draft.anniversary,
        onValueChange = { onDraftChange(draft.copy(anniversary = it)) },
        placeholder = stringResource(R.string.field_date_hint),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    Spacer(modifier = Modifier.height(12.dp))
    MetroListPicker(
        selected = draft.relationType,
        options = ContactEditorLogic.relationTypes.map {
            MetroListPickerOption(it.type, it.label)
        },
        onSelectedChange = { onDraftChange(draft.copy(relationType = it)) },
        label = stringResource(R.string.field_relation_type),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    FieldLabel(stringResource(R.string.field_relation))
    MetroTextBox(
        value = draft.relation,
        onValueChange = { onDraftChange(draft.copy(relation = it)) },
        placeholder = stringResource(R.string.field_relation),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )

    Spacer(modifier = Modifier.height(12.dp))
    FieldLabel(stringResource(R.string.field_notes))
    MetroTextBox(
        value = draft.notes,
        onValueChange = { onDraftChange(draft.copy(notes = it)) },
        placeholder = stringResource(R.string.field_notes),
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
    )
}

@Composable
private fun PhoneRows(
    phones: List<TypedContactValue>,
    onChange: (List<TypedContactValue>) -> Unit,
) {
    phones.forEachIndexed { index, phone ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        MetroListPicker(
            selected = phone.type,
            options = ContactEditorLogic.phoneTypes.map {
                MetroListPickerOption(it.type, it.label)
            },
            onSelectedChange = { type ->
                onChange(phones.toMutableList().also {
                    it[index] = phone.copy(type = type)
                })
            },
            label = stringResource(R.string.field_phone_type),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel(stringResource(R.string.field_phone))
        MetroTextBox(
            value = phone.value,
            onValueChange = { value ->
                onChange(phones.toMutableList().also {
                    it[index] = phone.copy(value = value)
                })
            },
            placeholder = stringResource(R.string.field_phone),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        if (phone.type == android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            FieldLabel(stringResource(R.string.field_custom_label))
            MetroTextBox(
                value = phone.customLabel,
                onValueChange = { label ->
                    onChange(phones.toMutableList().also {
                        it[index] = phone.copy(customLabel = label)
                    })
                },
                placeholder = stringResource(R.string.field_custom_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    MetroBorderButton(
        text = stringResource(R.string.add_phone),
        onClick = {
            onChange(
                phones + TypedContactValue(
                    type = android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ),
            )
        },
    )
}

@Composable
private fun EmailRows(
    emails: List<TypedContactValue>,
    onChange: (List<TypedContactValue>) -> Unit,
) {
    emails.forEachIndexed { index, email ->
        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
        MetroListPicker(
            selected = email.type,
            options = ContactEditorLogic.emailTypes.map {
                MetroListPickerOption(it.type, it.label)
            },
            onSelectedChange = { type ->
                onChange(emails.toMutableList().also {
                    it[index] = email.copy(type = type)
                })
            },
            label = stringResource(R.string.field_email_type),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel(stringResource(R.string.field_email))
        MetroTextBox(
            value = email.value,
            onValueChange = { value ->
                onChange(emails.toMutableList().also {
                    it[index] = email.copy(value = value)
                })
            },
            placeholder = stringResource(R.string.field_email),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        if (email.type == android.provider.ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            FieldLabel(stringResource(R.string.field_custom_label))
            MetroTextBox(
                value = email.customLabel,
                onValueChange = { label ->
                    onChange(emails.toMutableList().also {
                        it[index] = email.copy(customLabel = label)
                    })
                },
                placeholder = stringResource(R.string.field_custom_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    MetroBorderButton(
        text = stringResource(R.string.add_email),
        onClick = {
            onChange(
                emails + TypedContactValue(
                    type = android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME,
                ),
            )
        },
    )
}

@Composable
private fun FieldLabel(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.ListItemSubtitle,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
