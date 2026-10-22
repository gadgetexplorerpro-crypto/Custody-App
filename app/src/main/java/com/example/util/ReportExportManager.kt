package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CoParentMessage
import com.example.data.model.CustodyExchangeRecord
import com.example.data.model.CustodyScheduleEvent
import com.example.data.model.IncidentRecord
import com.example.data.model.SharedExpense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedReportFile(
    val file: File,
    val title: String,
    val mimeType: String,
    val summaryText: String
)

object ReportExportManager {

    fun generateComprehensivePdfReport(
        context: Context,
        familyId: String,
        parentName: String,
        coParentName: String,
        startDateIso: String,
        endDateIso: String,
        scheduleEvents: List<CustodyScheduleEvent>,
        exchanges: List<CustodyExchangeRecord>,
        incidents: List<IncidentRecord>,
        expenses: List<SharedExpense>,
        messages: List<CoParentMessage>
    ): ExportedReportFile {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeStart = startDateIso.ifBlank { "ALL" }.replace(Regex("[^0-9A-Za-z\\-]"), "")
        val safeEnd = endDateIso.ifBlank { "ALL" }.replace(Regex("[^0-9A-Za-z\\-]"), "")
        val outFile = File(reportsDir, "CustodyLog_Report_${safeStart}_to_${safeEnd}.pdf")

        val filteredSchedule = scheduleEvents.filter { inRange(it.dateIso, startDateIso, endDateIso) }
        val filteredExchanges = exchanges.filter { inRange(it.scheduledDateIso, startDateIso, endDateIso) }
        val filteredIncidents = incidents.filter { !it.isArchived && inRange(it.incidentDateIso, startDateIso, endDateIso) }
        val filteredExpenses = expenses.filter { inRange(it.dateIso, startDateIso, endDateIso) }

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 48f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1D3A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D9488")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 48f) {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = 48f
            }
        }

        val timestampNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        canvas.drawText("CustodyLog — Co-Parenting & Custody Documentation Report", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Workspace ID: $familyId | Prepared by: $parentName | Co-Parent: ${coParentName.ifBlank { "Not Linked" }}", 40f, y, bodyPaint)
        y += 15f
        canvas.drawText("Reporting Period: ${startDateIso.ifBlank { "All Dates" }} to ${endDateIso.ifBlank { "Present" }} | Generated: $timestampNow", 40f, y, metaPaint)
        y += 15f
        canvas.drawText("Notice: Records distinguish Scheduled, User-Reported, and Mutually Confirmed statuses for neutral mediation/legal review.", 40f, y, metaPaint)
        y += 12f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 22f

        // Section 1: Summary Metrics & Parenting Time Split
        canvas.drawText("1. Executive Summary & Parenting Time Calculation", 40f, y, sectionPaint)
        y += 18f
        val primaryScheduledCount = filteredSchedule.count { it.assignedParent == "PRIMARY" }
        val coParentScheduledCount = filteredSchedule.count { it.assignedParent == "CO_PARENT" }
        val totalCustodyBlocks = (primaryScheduledCount + coParentScheduledCount).coerceAtLeast(1)
        val primaryPct = (primaryScheduledCount * 100) / totalCustodyBlocks
        val onTimeExchanges = filteredExchanges.count { it.exchangeStatus == "COMPLETED_ON_TIME" }
        val lateExchanges = filteredExchanges.count { it.exchangeStatus == "LATE" }
        val missedExchanges = filteredExchanges.count { it.exchangeStatus == "MISSED" }
        val confirmedExchanges = filteredExchanges.count { it.verificationStatus == "MUTUALLY_CONFIRMED" }

        canvas.drawText(
            "Scheduled Blocks: ${filteredSchedule.size} (Primary Parent: $primaryScheduledCount [$primaryPct%], Co-Parent: $coParentScheduledCount [${100 - primaryPct}%])",
            40f, y, bodyPaint
        )
        y += 15f
        canvas.drawText(
            "Documented Exchanges: ${filteredExchanges.size} (On-Time: $onTimeExchanges, Late: $lateExchanges, Missed: $missedExchanges, Mutually Confirmed: $confirmedExchanges)",
            40f, y, bodyPaint
        )
        y += 15f
        canvas.drawText(
            "Incident Records: ${filteredIncidents.size} | Shared Expenses: ${filteredExpenses.size} | Logged Messages: ${messages.size}",
            40f, y, bodyPaint
        )
        y += 22f

        // Section 2: Custody Exchange History
        ensureSpace(40f)
        canvas.drawText("2. Custody Exchange History (With Verification Status)", 40f, y, sectionPaint)
        y += 16f
        if (filteredExchanges.isEmpty()) {
            canvas.drawText("No custody exchanges recorded in the selected date range.", 40f, y, metaPaint)
            y += 18f
        } else {
            filteredExchanges.take(25).forEach { ex ->
                ensureSpace(36f)
                val line1 = "${ex.scheduledDateIso} | Sched: ${ex.scheduledTime} | Actual: ${ex.actualArrivalTime} (${ex.latenessMinutes}m late) | Child: ${ex.childName}"
                val line2 = "Status: ${ex.exchangeStatus} | Verification: [${ex.verificationStatus}] | Logged by: ${ex.createdByName} | Loc: ${ex.location}"
                canvas.drawText(line1.take(105), 40f, y, bodyPaint)
                y += 13f
                canvas.drawText(line2.take(110), 40f, y, metaPaint)
                y += 16f
            }
        }

        // Section 3: Incident Timeline
        ensureSpace(40f)
        y += 8f
        canvas.drawText("3. Chronological Incident & Evidence Records", 40f, y, sectionPaint)
        y += 16f
        if (filteredIncidents.isEmpty()) {
            canvas.drawText("No incidents recorded in the selected date range.", 40f, y, metaPaint)
            y += 18f
        } else {
            filteredIncidents.take(25).forEach { inc ->
                ensureSpace(48f)
                canvas.drawText(
                    "${inc.incidentDateIso} ${inc.incidentTime} — [${inc.category}] ${inc.title} (Child: ${inc.childName})",
                    40f, y, bodyPaint
                )
                y += 13f
                canvas.drawText(
                    "Verification: [${inc.verificationStatus}] | Revision: v${inc.version} | Attachment: ${inc.attachmentType} (${inc.attachmentName.ifBlank { "None" }})",
                    40f, y, metaPaint
                )
                y += 13f
                canvas.drawText("Details: ${inc.description.take(100)}", 40f, y, metaPaint)
                y += 16f
            }
        }

        // Section 4: Shared Expense Statement
        ensureSpace(40f)
        y += 8f
        canvas.drawText("4. Shared Expense Statement", 40f, y, sectionPaint)
        y += 16f
        if (filteredExpenses.isEmpty()) {
            canvas.drawText("No shared expenses recorded in the selected date range.", 40f, y, metaPaint)
            y += 18f
        } else {
            filteredExpenses.take(25).forEach { exp ->
                ensureSpace(32f)
                val sym = if (exp.currency == "GBP") "£" else "$"
                val totalFormatted = String.format(Locale.US, "%s%.2f", sym, exp.totalAmountCents / 100.0)
                val coShareFormatted = String.format(Locale.US, "%s%.2f", sym, exp.coParentShareCents / 100.0)
                canvas.drawText(
                    "${exp.dateIso} | ${exp.title} (${exp.category}) | Total: $totalFormatted | Co-Parent Share: $coShareFormatted | Status: [${exp.paymentStatus}]",
                    40f, y, bodyPaint
                )
                y += 15f
            }
        }

        // Section 5: Co-Parent Communication Log
        ensureSpace(40f)
        y += 8f
        canvas.drawText("5. Co-Parent Communication Transcript & Audit Trail", 40f, y, sectionPaint)
        y += 16f
        if (messages.isEmpty()) {
            canvas.drawText("No messages recorded.", 40f, y, metaPaint)
            y += 18f
        } else {
            messages.takeLast(25).forEach { msg ->
                ensureSpace(32f)
                val auditFlag = when {
                    msg.isRetracted -> "[RETRACTED - Original: ${msg.originalBody.take(40)}]"
                    msg.isEdited -> "[EDITED - Original: ${msg.originalBody.take(40)}]"
                    else -> "[ORIGINAL]"
                }
                canvas.drawText(
                    "${msg.senderName}: ${msg.body.take(75)} $auditFlag",
                    40f, y, bodyPaint
                )
                y += 14f
            }
        }

        pdf.finishPage(page)
        FileOutputStream(outFile).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()

        return ExportedReportFile(
            file = outFile,
            title = outFile.name,
            mimeType = "application/pdf",
            summaryText = "PDF generated with ${filteredSchedule.size} schedule blocks, ${filteredExchanges.size} exchanges, ${filteredIncidents.size} incidents, ${filteredExpenses.size} expenses, and ${messages.size} messages."
        )
    }

    fun generateCsvReport(
        context: Context,
        startDateIso: String,
        endDateIso: String,
        exchanges: List<CustodyExchangeRecord>,
        incidents: List<IncidentRecord>,
        expenses: List<SharedExpense>
    ): ExportedReportFile {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(reportsDir, "CustodyLog_Records_And_Expenses.csv")

        val filteredExchanges = exchanges.filter { inRange(it.scheduledDateIso, startDateIso, endDateIso) }
        val filteredIncidents = incidents.filter { !it.isArchived && inRange(it.incidentDateIso, startDateIso, endDateIso) }
        val filteredExpenses = expenses.filter { inRange(it.dateIso, startDateIso, endDateIso) }

        val sb = StringBuilder()
        sb.appendLine("RecordType,DateISO,ChildName,CategoryOrStatus,VerificationStatus,TitleOrLocation,AmountOrLateness,LoggedBy,NotesOrAudit")
        filteredExchanges.forEach { ex ->
            sb.appendLine(
                listOf(
                    "EXCHANGE",
                    ex.scheduledDateIso,
                    ex.childName,
                    ex.exchangeStatus,
                    ex.verificationStatus,
                    ex.location,
                    "${ex.latenessMinutes}m late",
                    ex.createdByName,
                    ex.notes
                ).joinToString(",") { escapeCsv(it) }
            )
        }
        filteredIncidents.forEach { inc ->
            sb.appendLine(
                listOf(
                    "INCIDENT",
                    inc.incidentDateIso,
                    inc.childName,
                    inc.category,
                    inc.verificationStatus,
                    inc.title,
                    "v${inc.version}",
                    inc.createdByName,
                    "${inc.description} (${inc.auditHistorySummary})"
                ).joinToString(",") { escapeCsv(it) }
            )
        }
        filteredExpenses.forEach { exp ->
            val sym = if (exp.currency == "GBP") "GBP " else "USD "
            val amountStr = String.format(Locale.US, "%s%.2f (Co-Parent: %.2f)", sym, exp.totalAmountCents / 100.0, exp.coParentShareCents / 100.0)
            sb.appendLine(
                listOf(
                    "EXPENSE",
                    exp.dateIso,
                    exp.childName,
                    exp.category,
                    exp.paymentStatus,
                    exp.title,
                    amountStr,
                    exp.createdByName,
                    exp.statusNote
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        outFile.writeText(sb.toString())
        return ExportedReportFile(
            file = outFile,
            title = outFile.name,
            mimeType = "text/csv",
            summaryText = "CSV exported (${filteredExchanges.size} exchanges, ${filteredIncidents.size} incidents, ${filteredExpenses.size} expenses)."
        )
    }

    fun shareExportedFile(context: Context, exported: ExportedReportFile) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exported.file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = exported.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CustodyLog Documentation Export — ${exported.title}")
            putExtra(Intent.EXTRA_TEXT, exported.summaryText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export CustodyLog Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun inRange(dateIso: String, startIso: String, endIso: String): Boolean {
        if (dateIso.isBlank()) return true
        if (startIso.isNotBlank() && dateIso < startIso) return false
        if (endIso.isNotBlank() && dateIso > endIso) return false
        return true
    }

    private fun escapeCsv(value: String): String {
        val sanitized = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$sanitized\""
    }
}
