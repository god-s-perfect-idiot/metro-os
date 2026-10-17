package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PersonDetail
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.coroutines.launch

@Composable
fun ContactDetailScreen(
    detail: PersonDetail,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onText: () -> Unit,
    onWhatsAppCall: () -> Unit,
    onWhatsAppText: () -> Unit,
    onEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = detail.summary
    val titles = buildList {
        add(stringResource(R.string.pivot_profile))
        if (detail.hasMoreInfo) add(stringResource(R.string.pivot_more_info))
    }
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    MetroPivot(
        titles = titles,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
            ) {
                MetroText(
                    text = summary.displayName.uppercase(),
                    style = MetroTextStyle.SectionHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MetroText(
                    text = summary.sourceLabel,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        },
        onTitleClick = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
        pageContent = { page ->
            when (page) {
                0 -> ProfilePivot(
                    detail = detail,
                    onCall = onCall,
                    onText = onText,
                    onWhatsAppCall = onWhatsAppCall,
                    onWhatsAppText = onWhatsAppText,
                    onEmail = onEmail,
                )
                else -> MoreInfoPivot(detail = detail)
            }
        },
    )
}

@Composable
private fun ProfilePivot(
    detail: PersonDetail,
    onCall: () -> Unit,
    onText: () -> Unit,
    onWhatsAppCall: () -> Unit,
    onWhatsAppText: () -> Unit,
    onEmail: (String) -> Unit,
) {
    val summary = detail.summary
    val whatsApp = detail.whatsApp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Row(modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)) {
            ContactAvatar(
                contactId = summary.id,
                modifier = Modifier.size(200.dp),
            )
        }

        ActionRow(
            label = stringResource(R.string.call_mobile),
            value = summary.defaultPhone ?: "—",
            enabled = summary.defaultPhone != null,
            onClick = onCall,
        )
        ActionRow(
            label = stringResource(R.string.text),
            value = "SMS",
            enabled = summary.defaultPhone != null,
            onClick = onText,
        )
        if (whatsApp?.canVoiceCall == true) {
            ActionRow(
                label = stringResource(R.string.call_whatsapp),
                value = stringResource(R.string.whatsapp_label),
                enabled = true,
                onClick = onWhatsAppCall,
            )
        }
        if (whatsApp?.canMessage == true) {
            ActionRow(
                label = stringResource(R.string.text_whatsapp),
                value = stringResource(R.string.whatsapp_label),
                enabled = true,
                onClick = onWhatsAppText,
            )
        }
        summary.defaultEmail?.let { email ->
            ActionRow(
                label = stringResource(R.string.send_email),
                value = email,
                enabled = true,
                onClick = { onEmail(email) },
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun MoreInfoPivot(detail: PersonDetail) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp),
    ) {
        InfoRow(stringResource(R.string.field_name_prefix), detail.namePrefix)
        InfoRow(stringResource(R.string.field_name), detail.givenName)
        InfoRow(stringResource(R.string.field_middle_name), detail.middleName)
        InfoRow(stringResource(R.string.field_last_name), detail.familyName)
        InfoRow(stringResource(R.string.field_name_suffix), detail.nameSuffix)
        InfoRow(stringResource(R.string.field_phonetic_name), detail.phoneticName)
        InfoRow(stringResource(R.string.field_nickname), detail.nickname)

        detail.phones.forEach { phone ->
            InfoRow(phone.label, phone.value)
        }
        detail.emails.forEach { email ->
            InfoRow(email.label, email.value)
        }

        InfoRow(stringResource(R.string.field_company), detail.company)
        InfoRow(stringResource(R.string.field_job_title), detail.jobTitle)
        InfoRow(stringResource(R.string.field_department), detail.department)

        detail.postal?.takeIf { it.hasContent }?.let { postal ->
            InfoRow(postal.typeLabel, postal.formattedLines().joinToString("\n"))
        }

        InfoRow(stringResource(R.string.field_website), detail.website)
        if (detail.imHandle.isNotBlank()) {
            val label = detail.imProtocolLabel.ifBlank { stringResource(R.string.field_im) }
            InfoRow(label, detail.imHandle)
        }
        InfoRow(stringResource(R.string.field_birthday), detail.birthday)
        InfoRow(stringResource(R.string.field_anniversary), detail.anniversary)
        if (detail.relation.isNotBlank()) {
            val label = detail.relationLabel.ifBlank { stringResource(R.string.field_relation) }
            InfoRow(label, detail.relation)
        }
        InfoRow(stringResource(R.string.field_notes), detail.notes)

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.metroClickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemSubtitle,
            color = if (enabled) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
        )
    }
}
