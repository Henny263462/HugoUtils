package dev.henny.hugoutils.ui

class TextFieldLogic(
    text: String = "",
    var maxLength: Int = Int.MAX_VALUE
) {
    var text: String = text.take(maxLength)
        private set
    var cursor: Int = this.text.length
        private set
    var anchor: Int = cursor
        private set
    val selection: IntRange? get() {
        if (cursor == anchor) return null
        return minOf(cursor, anchor) until maxOf(cursor, anchor)
    }
    val selectedText: String get() = selection?.let { text.substring(it.first, it.last + 1) }.orEmpty()

    fun setText(value: String) {
        text = value.take(maxLength)
        cursor = text.length
        anchor = cursor
    }

    fun selectAll() {
        anchor = 0
        cursor = text.length
    }

    fun moveCursor(offset: Int, selecting: Boolean = false) {
        cursor = (cursor + offset).coerceIn(0, text.length)
        if (!selecting) anchor = cursor
    }

    fun insert(value: String) {
        deleteSelection()
        val insertion = value.take((maxLength - text.length).coerceAtLeast(0))
        text = text.substring(0, cursor) + insertion + text.substring(cursor)
        cursor += insertion.length
        anchor = cursor
    }

    fun backspace() {
        if (deleteSelection()) return
        if (cursor > 0) {
            text = text.removeRange(cursor - 1, cursor)
            cursor--
            anchor = cursor
        }
    }

    fun delete() {
        if (deleteSelection()) return
        if (cursor < text.length) text = text.removeRange(cursor, cursor + 1)
        anchor = cursor
    }

    fun copy(): String = selectedText
    fun cut(): String = copy().also { deleteSelection() }
    fun paste(value: String) = insert(value)

    private fun deleteSelection(): Boolean {
        val range = selection ?: return false
        text = text.removeRange(range.first, range.last + 1)
        cursor = range.first
        anchor = cursor
        return true
    }
}
