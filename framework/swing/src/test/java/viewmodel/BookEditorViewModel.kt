package viewmodel

import androidx.core.bundle.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import model.Author
import model.Book
import model.Genre
import kotlin.reflect.KClass


class BookEditorViewModel(
    val value: Int,
    val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    fun save(book: Book): Bundle {
        val data = Bundle()
        data.putString("Book Title", book.name)
        data.putString("Author", book.author.name)
        data.putString("Genre", book.genre.name)
        data.putBoolean("Is Unavailable", book.isTaken)
        savedStateHandle.set<Bundle>("data", data)
        return data
    }

    fun parse(bundle: Bundle): Book {
        val name = bundle.getString("Book Name")!!
        val author = bundle.getString("Author")!!
        val genre = bundle.getString("Genre")!!
        val isTaken = bundle.getBoolean("Is Unavailable", false)
        return Book(
            Author(author,""),
            Genre.valueOf(genre),
            name,
        ).apply {
            this.isTaken = isTaken
        }
    }


    fun print() {
        println("hello")
    }

    companion object {
        val creationExtrasKey1 = object : CreationExtras.Key<Int> {}

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                val intValue = extras[creationExtrasKey1]!!
                val savedStateHandle = extras.createSavedStateHandle()
                val vm = BookEditorViewModel(intValue, savedStateHandle)
                return vm as T
            }
        }
    }
}
