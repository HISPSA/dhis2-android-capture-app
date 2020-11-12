package org.dhis2.data.forms.dataentry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.processors.PublishProcessor
import org.dhis2.data.forms.dataentry.fields.section.SectionHolder
import org.dhis2.data.forms.dataentry.fields.section.SectionViewModel

const val NO_POSITION = -1

class DataEntryHeaderHelper(
    private val headerContainer: ViewGroup,
    private val recyclerView: RecyclerView
) {
    private val currentSection = MutableLiveData<SectionViewModel>()

    fun observeHeaderChanges(owner: LifecycleOwner) {
        currentSection.observe(
            owner,
            Observer { section: SectionViewModel? ->
                this.loadHeader(
                    section
                )
            }
        )
    }

    fun checkSectionHeader(recyclerView: RecyclerView) {
        val dataEntryAdapter = recyclerView.adapter as DataEntryAdapter
        val visiblePos = when (recyclerView.layoutManager) {
            is GridLayoutManager ->
                (recyclerView.layoutManager as GridLayoutManager?)
                    ?.findFirstVisibleItemPosition()
            else ->
                (recyclerView.layoutManager as LinearLayoutManager?)
                    ?.findFirstVisibleItemPosition()
        } ?: NO_POSITION

        if (visiblePos != NO_POSITION && dataEntryAdapter.sectionSize > 1) {
            dataEntryAdapter.getSectionForPosition(visiblePos)?.let { headerSection ->
                if (headerSection.isOpen && !dataEntryAdapter.isSection(visiblePos + 1)) {
                    if (currentSection.value == null || currentSection.value!!
                            .uid() != headerSection.uid()
                    ) {
                        currentSection.value = headerSection
                    }
                } else {
                    currentSection.setValue(null)
                }
            }
        }
    }

    private fun loadHeader(section: SectionViewModel?) {
        val dataEntryAdapter = recyclerView.adapter as DataEntryAdapter
        if (section != null && section.isOpen) {
            val layoutInflater = LayoutInflater.from(headerContainer.context)
            val binding =
                DataBindingUtil.inflate<ViewDataBinding>(
                    layoutInflater,
                    section.getLayoutId(),
                    headerContainer,
                    false
                )
            val sectionHolder: SectionHolder =
                SectionHolder(binding, ObservableField(String()), PublishProcessor.create())
            val sectionPosition: Int = dataEntryAdapter.getSectionPosition(section.uid())
            dataEntryAdapter.updateSectionData(sectionHolder, sectionPosition, true)
            headerContainer.removeAllViews()
            headerContainer.addView(sectionHolder.itemView)
            sectionHolder.update(section)
        } else {
            headerContainer.removeAllViews()
        }
    }

    fun onItemsUpdatedCallback() {
        val dataEntryAdapter = recyclerView.adapter as DataEntryAdapter
        if (currentSection.value != null) {
            loadHeader(
                dataEntryAdapter.getSectionForPosition(
                    dataEntryAdapter.getSectionPosition(currentSection.value!!.uid())
                )
            )
        }
    }
}
