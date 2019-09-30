package net.ocsoft.mswp.ui

import kotlin.math.min
import jQuery
import JQueryEventObject
import JQueryStatic
import JQuery
import org.w3c.dom.Element  
import fontawesome.Icons

import kotlin.text.toIntOrNull
import kotlin.browser.window
import kotlin.collections.List
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Map
import kotlin.collections.HashMap
import net.ocsoft.mswp.Activity
import net.ocsoft.mswp.*

/**
 * icon selector
 */
class IconSelector(val option : Option) {
    /**
     * option 
     */
    data class Option(val modalQuery: String, 
        val itemListQuery: String,
        val itemsQuery: String,
        val pagingContainerQuery: String,
        val okQuery: String,
        /**
         * runtime icon item query
         */
        val rtIconItemQuery: String,
        val itemTemplateQuery: String,
        val blankItemTemplateQuery: String,
        val pagingCtrlFullTemplateQuery: String,
        val pagingCtrlMediumTemplateQuery: String,
        val pagingCtrlSimpleTemplateQuery: String,
        val pagingItemTemplateQuery: String,
        val syncIconTemplateQuery: String,
        val iconSetting: IconSetting)

    /**
     * icon page contents meta infomation
     */
    data class ContentsMeta(
        val colSize: Int,
        val rowSize: Int,
        val iconIdentifiers: List<Icons.Identifier>)
    

    /**
     * runtime configuration
     */
    var runtimeConfig: dynamic? = null

    /**
     * contents meta information
     */
    var contentsMeta: ContentsMeta? = null

    /**
     * count of icon items in page
     */
    val countOfIconItemsInPage: Int
        get() {
            val items = jQuery(option.itemsQuery)
            return items.length.toInt()
        }

    /**
     * handle number control event
     */
    var numberCtrlHdlr: ((JQueryEventObject, Any?)->Any)? = null
    
    /**
     * handle go to fist page event
     */
    var firstPageHdlr: ((JQueryEventObject, Any?)->Any)? = null
     
    /**
     * handle go to last page
     */
    var lastPageHdlr: ((JQueryEventObject, Any?)->Any)? = null 

    /**
     * handle previous pages ui event 
     */
    var prevPageHdlr: ((JQueryEventObject, Any?)->Any)? = null

    /**
     * handle next pages ui event 
     */
    var nextPageHdlr: ((JQueryEventObject, Any?)->Any)?  = null
 
    /**
     * ok button handler
     */
    var okHdlr: ((JQueryEventObject, Any?)->Any)? = null

    /**
     * icon item click handler
     */
    var iconItemClickHdlr: ((JQueryEventObject, Any?)->Any)? = null

    /**
     * called when modal dialog is hidden.
     */
    var modalHiddenHdlr: ((JQueryEventObject, Any?)->Any)? = null
    
    /**
     * contens of ok ui control, this list is keeping while ajax request. 
     */
    var okContents: MutableList<String>? = null 


    /**
     * currently selected icon 
     */
    var selectedMineIcon: Persistence.Icon? = null

    
    /**
     * bind into html
     */
    fun bind() {
    }

    /**
     * unbind from html
     */
    fun unbind() {
    }


    /**
     * visible modal dialog
     */
    fun show() {
        bindModal()
        val modalObj = jQuery(option.modalQuery)
        modalObj.asDynamic().modal("show") 
    }
    /**
     * connect functions into modal user interface.
     */
    fun bindModal() {
        modalHiddenHdlr = { e, arg -> onModalHidden(e, arg) }
        okHdlr = { e, arg -> onOk(e, arg) }
        iconItemClickHdlr = { e, args -> onIconItemClick(e, args) }
 
        val modalQuery = jQuery(option.modalQuery) as JQuery
        jQuery(option.okQuery).on("click", okHdlr!!)
        modalQuery.on("hidden.bs.modal", modalHiddenHdlr!!)
        selectedMineIcon = option.iconSetting.mineIcon 

        setupContents()
    }

    /**
     * disconnect functions from modal user interface
     */
    fun unbindModal() {
        destroyContents()
        if (modalHiddenHdlr != null) {
            jQuery(option.modalQuery).off("hidden.bs.modal", modalHiddenHdlr!!)
            modalHiddenHdlr = null
        }

        if (okHdlr != null) {
            jQuery(option.okQuery).off("click", okHdlr!!)
            okHdlr = null
        }
        selectedMineIcon = null
        iconItemClickHdlr = null
    }

    /**
     * handle modal hidden event
     */
    fun onModalHidden(e: JQueryEventObject, a: Any?): Any {
        unbindModal() 
        return Unit
    }

    /**
     * handle ok
     */
    fun onOk(e: JQueryEventObject, args: Any?): Any {
                
        postSave()
        return Unit
    }

    /**
     * save data into host 
     */
    fun postSave() {
        window.setTimeout({
            doSave()
        }, 100)
    }

    /**
     * save setting into server
     */
    fun doSave() {
        changeUiOkToSync()
        val selectedIcon = selectedMineIcon!!
        Persistence.saveIcon(selectedIcon).then({
            jQuery(option.modalQuery).asDynamic().modal("hide")
            restoreOkFromSync()
            option.iconSetting.mineIcon = selectedIcon
        }, {
            // todo: you have to display waring 
            restoreOkFromSync()   
        })
   } 

   
    /**
     * change ok to synchronizing icon
     */
    fun changeUiOkToSync() {
        val syncHtml = jQuery(option.syncIconTemplateQuery).html()

        okContents = ArrayList<String>()
        jQuery(option.okQuery).html({
            index, oldHtml ->
            okContents?.add(oldHtml) 
            syncHtml
        })
    }
    /**
     * restore ok from synchronizing icon
     */
    fun restoreOkFromSync() {
        val okContents = this.okContents
        if (okContents != null) {
            jQuery(option.okQuery).html({
                index, oldHtml ->
                okContents[index.toInt()]
            })
            this.okContents = null
        } 
    }
       

    /**
     * set up user interface contents
     */
    fun setupContents() {
        var ids = readIconIdentfiers() 
        var tableSize = findTableSize()
        if (tableSize != null) {
            var (colSize, rowSize) = tableSize
            if (colSize != null && rowSize != null) {
                contentsMeta = ContentsMeta(colSize, rowSize, ids)
                val pageNumber = findPageSelectedItemIn(contentsMeta!!)
                updatePage(pageNumber, 
                    contentsMeta!!)
                setupPagination(pageNumber)
                postFocusInNumberPage(pageNumber)
            }
        }
    }

    /**
     * find the page in which selected icon is.
     */
    fun findPageSelectedItemIn(ctMeta: ContentsMeta): Int {
        var result = 0
        if (selectedMineIcon != null) {
                
            val iconPageMap = createIconPageMap(ctMeta) 
            result = iconPageMap[selectedMineIcon!!] ?: 0
        }
        
        return result 
    }
    
    /**
     * create icon page map
     */
    fun createIconPageMap(ctMeta: ContentsMeta): Map<Persistence.Icon, Int> {
        val result = HashMap<Persistence.Icon, Int>()
        val itemCountInPage = ctMeta.colSize * ctMeta.rowSize
        ctMeta.iconIdentifiers.forEachIndexed {
            index, elem ->
            val pageIdx = index / itemCountInPage
            val icon = Persistence.Icon(elem.prefix, elem.name)
            result[icon] = pageIdx
        }

        return result
    }

    /**
     * read icon identifier list
     */
    fun readIconIdentfiers(): List<Icons.Identifier> {
        var ids = Icons.getAllIdentifiersCodeOrder() 
        val rc = runtimeConfig["icon_list"]
        var result = ids
        if (rc != null) {
            if (rc.icons != null) {
                if (rc.icons.exclude != null) {
                    if (rc.icons.exclude["font-awesome"] != null) {
                        val excCode = HashSet<String>() 
                        val faEx = rc.icons.exclude["font-awesome"]
                        for (i in 0..faEx.length - 1) {
                            excCode.add(faEx[i].code) 
                        }
                        result = ids.filter({ it.code !in excCode })
                    }
                }
            }
        }
        return result
    }
    /**
     * setup pagination ui
     */
    fun setupPagination(firstPageNumber: Int = 0) {
        var ctMeta = contentsMeta
        if (ctMeta != null) {
            val pageCount = calcCountOfPages(ctMeta)       
            if (pageCount > 1) {
                val pagingSize = findPagingSize()
                if (pagingSize != null) {
                    var templateQuery = option.pagingCtrlSimpleTemplateQuery

                    if (pageCount > pagingSize) {
                        val pagingCount = pageCount / pagingSize
                        if (pagingCount > 2) {
                            templateQuery = option.pagingCtrlFullTemplateQuery 
                        } else {
                            templateQuery = 
                                option.pagingCtrlMediumTemplateQuery 
                        } 
                    } 
                    setupPagingCtrl(templateQuery, 
                        Pair(firstPageNumber, pagingSize))
                } 
            }
        } 
    }

    /**
     * update pagination ui
     */
    fun updatePagination(
        pageIndex: Int) {
    }

    /**
     * create paging control
     */
    fun setupPagingCtrl(templateQuery: String,
        page: Pair<Int, Int>) {
        val (startIndex, size) = page
        
        val pagingCtrlStr = jQuery(templateQuery).html()
        val pagingCtrl = jQuery(pagingCtrlStr)
        val insertEndIdx = pagingCtrl.children().length.toInt() / 2 
        for (i in startIndex..startIndex + size - 1) {
            val pagingItemStr = jQuery(option.pagingItemTemplateQuery).html()
            val pagingItem = jQuery(pagingItemStr)
            pagingItem.html("${i + 1}")
            val insertIdx = pagingCtrl.children().length.toInt() - insertEndIdx
            pagingCtrl.children().eq(insertIdx).before(pagingItem) 
        }
        unbindPagingCtrl()
        val pagingContainer = jQuery(option.pagingContainerQuery)
        if (pagingContainer.children().length.toInt() > 0) {
            pagingContainer.children().eq(0).replaceWith(pagingCtrl)
        } else {
            pagingContainer.append(pagingCtrl)
        }

        bindPagingCtrl()
    }

    /**
     * bind paging user interface.
     */
    fun bindPagingCtrl() {
        firstPageHdlr = { e, args -> onFirstPage(e, args) }
        lastPageHdlr = { e, args -> onLastPage(e, args) }
        prevPageHdlr = { e, args -> onPrevPage(e, args) }
        nextPageHdlr = { e, args -> onNextPage(e, args) }
        numberCtrlHdlr = { e, args -> onNumberUi(e, args) }

        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl = pagingContainer.children().eq(0) as JQuery?

        jQuery(".first", pagingCtrl).on("click", firstPageHdlr!!)
        jQuery(".last", pagingCtrl).on("click", lastPageHdlr!!)
        jQuery(".prev", pagingCtrl).on("click", prevPageHdlr!!)
        jQuery(".next", pagingCtrl).on("click", nextPageHdlr!!)
        jQuery(".page-number", pagingCtrl).on("click", numberCtrlHdlr!!)
    }


    /**
     * unbind handler from paging control
     */
    fun unbindPagingCtrl() {
        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl = pagingContainer.children().eq(0) as JQuery?

        if (firstPageHdlr != null) {
            jQuery(".first", pagingCtrl).off("click", firstPageHdlr!!)
        }
        if (lastPageHdlr != null) {
            jQuery(".last", pagingCtrl).off("click", lastPageHdlr!!)
        }
        if (prevPageHdlr != null) {
            jQuery(".prev", pagingCtrl).off("click", prevPageHdlr!!)
        }
        if (nextPageHdlr != null) {
            jQuery(".next", pagingCtrl).off("click", nextPageHdlr!!)
        }
        if (numberCtrlHdlr != null) {
            jQuery(".page-number", pagingCtrl).off("click", numberCtrlHdlr!!)
        }

        firstPageHdlr = null
        lastPageHdlr = null 
        prevPageHdlr = null
        nextPageHdlr = null
        numberCtrlHdlr = null
        selectedMineIcon = null
     }


    /**
     * handle number control  
     */
    fun onNumberUi(e: JQueryEventObject, args: Any?): Any {
        val ctMeta = contentsMeta
        if (ctMeta != null) {
            postUpdatePage(
                jQuery(e.delegateTarget).text().toInt() - 1,
                ctMeta!!)
            Activity.record()
        }
        return Unit
    }

    /**
     * handle go to fist page event
     */
    fun onFirstPage(e :JQueryEventObject, args: Any?): Any {
        postRenumberPageCtrl(0) 
        Activity.record()
        return Unit
    }
     
    /**
     * handle go to last page
     */
    fun onLastPage(e :JQueryEventObject, args: Any?): Any {
        val ctMeta = contentsMeta
        if (ctMeta != null) {
            val pageUiCount = calcCountOfPageNumUi()
            val pageCount = calcCountOfPages(ctMeta)
            val nextIndex = pageCount - pageUiCount
            postRenumberPageCtrl(nextIndex)
            Activity.record()
        }
        return Unit
    }

    /**
     * handle previous pages ui event 
     */
    fun onPrevPage(e : JQueryEventObject, args: Any?): Any {
        val pageRange = readPageNumbersFromUi()
        if (pageRange != null) {
            val pageUiCount = calcCountOfPageNumUi()
            var prevIndex = pageRange.first - pageUiCount + 1
            if (prevIndex < 0) {
                prevIndex = 0
            }
            postRenumberPageCtrl(prevIndex)
            Activity.record()
        }
        return Unit
    }

    /**
     * handle next pages ui event 
     */
    fun onNextPage(e : JQueryEventObject, args: Any?): Any {
        val pageRange = readPageNumbersFromUi()
        val ctMeta = contentsMeta
        if (pageRange != null && ctMeta != null) {
            val pageUiCount = calcCountOfPageNumUi()           
            var nextIndex = pageRange.second 
            val endIndex = nextIndex + pageUiCount - 1
            val pageCount = calcCountOfPages(ctMeta)
            if (endIndex > pageCount) {
                nextIndex = pageCount - pageUiCount
            }
            postRenumberPageCtrl(nextIndex)
            Activity.record()
        }
        return Unit
    }


    /**
     * renumber page index
     */    
    fun postRenumberPageCtrl(pageIndex: Int) {
        window.setTimeout({
            renumberPageCtrl(pageIndex)
        })    
    } 

    /**
     * renumber page control interface
     */
    fun renumberPageCtrl(pageIndex: Int) {
        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl =  pagingContainer.children().eq(0) as JQuery?
        val pageNumbers = jQuery(".page-number", pagingCtrl)
        pageNumbers.each({ idx, elem -> 
            val pageNumber = pageIndex + idx.toInt() + 1
            jQuery(elem).text("${pageNumber}") 
            true
        })
    } 

    /**
     * focus in pageNumber if pageIndex is in numbers lateley.
     */
    fun postFocusInNumberPage(pageIndex: Int) {
        window.setTimeout({
            focusInNumberPage(pageIndex)
        }, 100)
    }

    /**
     * focus in pageNumber if pageIndex is in numbers.
     */
    fun focusInNumberPage(pageIndex: Int) {
        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl =  pagingContainer.children().eq(0) as JQuery?
        val pageNumbers = jQuery(".page-number", pagingCtrl)
        val pageNumber = pageIndex + 1
        var elemIndex : Int? = null
        pageNumbers.each { idx, elem -> 
            val elemNum = jQuery(elem).text().toIntOrNull()
            var notMatched = true
            if (elemNum != null) {
                notMatched = elemNum!! != pageNumber  
                if (!notMatched) {
                    elemIndex = idx.toInt()
                }
            }
            notMatched 
        }
        if (elemIndex != null) {
            pageNumbers.eq(elemIndex!!).trigger("focus")
        }

    }

    

    /**
     * read page numbers from user inteface
     */
    fun readPageNumbersFromUi(): Pair<Int, Int>? {
        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl =  pagingContainer.children().eq(0) as JQuery? 
        val pageNumbers = jQuery(".page-number", pagingCtrl)
        val firstNode = pageNumbers.eq(0)
        val lastNode = pageNumbers.eq(pageNumbers.length.toInt() - 1)

        var result : Pair<Int, Int>? = null
        val firstNum = firstNode.text().toIntOrNull()
        val lastNum = lastNode.text().toIntOrNull()
        if (firstNum != null && lastNum != null) {
            result = Pair(firstNum - 1, lastNum - 1)
        }        
        return result
    }

    /**
     * update icon page later
     */
    fun postUpdatePage(pageIndex: Int,
        contentsMeta: ContentsMeta,
        selectionInPage: Int? = null) {
        window.setTimeout({
            updatePage(pageIndex, contentsMeta, selectionInPage)
        }, 100)
    } 
    
    /**
     * update icon  page
     */
    fun updatePage(pageIndex : Int,
        contentsMeta: ContentsMeta,
        selectionInPage: Int? = null) {
        val ids = contentsMeta.iconIdentifiers
        val iconCount = contentsMeta.colSize * contentsMeta.rowSize
        val pageCount = (ids.size + iconCount - 1) / iconCount
       
        while (countOfIconItemsInPage > iconCount) {
            removeItem(countOfIconItemsInPage - 1)
        } 

        if (0 <= pageIndex && pageIndex < pageCount) {
            val iconStart = iconCount * pageIndex
            val iconEnd = (iconCount * (pageIndex + 1)) - 1
            for (i in  iconStart..iconEnd) {  
                var iconElem: Icons.Identifier? = null
                var idxInPage = i - iconStart
                if (i < ids.size) {
                    iconElem = ids[i]
                }
                replaceItem(iconElem, idxInPage)
            }
        }
    } 

    /**
     * calc count of page button ui
     */
    fun calcCountOfPageNumUi(): Int {
        val pagingContainer = jQuery(option.pagingContainerQuery)
        val pagingCtrl =  pagingContainer.children().eq(0) as JQuery? 
        val pageNumbers = jQuery(".page-number", pagingCtrl)
        val result = pageNumbers.length.toInt()
        return result 
    }
    /**
     * calculate count of pages
     */
    fun calcCountOfPages(contentsMeta: ContentsMeta): Int {
        val ids = contentsMeta.iconIdentifiers
        val iconCount = contentsMeta.colSize * contentsMeta.rowSize
        val result = (ids.size + iconCount - 1) / iconCount
        return result
    }
    /**
     * tear down user interface contents
     */
    fun destroyContents() {
        val items = jQuery(option.itemListQuery)
        items.empty()
        val pagingContainer = jQuery(option.pagingContainerQuery)
        pagingContainer.empty() 
    } 

    /**
     * find table size from runtime configuration
     */
    fun findTableSize(): Pair<Int?, Int?>? {
        var result : Pair<Int?, Int?>? = null
        val rc = runtimeConfig["icon_list"]
        if (rc != null) {
            var rowCount : Int? = null 
            if (rc.rowSize != null) {
                rowCount = Responsive.findRowCount(
                    rc.rowSize,
                    "minHeight", "maxHeight")
            }
            if (rowCount == null) {
                rowCount = rc.rowSizeDefault
            }

            var colCount : Int? = null
            if (rc.colSize != null) {
                colCount = Responsive.findRowCount(
                    rc.ColSize,
                    "minWidth", "maxWidth") 
            }
            if (colCount == null) {
                colCount = rc.colSizeDefault
            }
            result = Pair(colCount, rowCount)
        } 

        return result 
    }

    /**
     * find paging size from runtime configuration
     */
    fun findPagingSize(): Int? {
        var result : Int? = null
        val rc = runtimeConfig["icon_list"]
        if (rc != null) {
            if (rc.pagingSize != null) {
                result = Responsive.findXCount(
                    rc.pagingSize,
                    "minWidth", "maxWidth")
            }
            if (result == null) {
                result = rc.pagingSizeDefault
            }
        }
        return result
    }

 
    /**
     * append item
     */
    fun appendItem(item : Icons.Identifier?) {
        replaceItem(item, countOfIconItemsInPage)
    }
    /**
     * replace item
     */
    fun replaceItem(item: Icons.Identifier?,
        index: Int) {
        var items = jQuery(option.itemsQuery)
        if (0 <= index && index <= items.length.toInt()) {
            var newNode : JQuery? = null
            var selected = false
            if (item != null) {
                val newLine = jQuery(option.itemTemplateQuery).html()
                newNode = jQuery(newLine) 
                val divNode = jQuery("div", newNode as JQueryStatic)
                val iNode = jQuery("i", divNode as JQueryStatic)
                iNode.addClass("${item.prefix} fa-${item.name}")
                selected = selectedMineIcon == Persistence.Icon(
                    item.prefix, item.name)
            } else {
                val newLine = jQuery(option.blankItemTemplateQuery).html()
                newNode = jQuery(newLine) 
            }
            if (index < items.children().length.toInt()) {
                if (iconItemClickHdlr != null) {
                    items.eq(index).off(
                        "click", iconItemClickHdlr!!) 
                }
                items.eq(index).replaceWith(newNode) 
                items = jQuery(option.itemsQuery)
            } else {
                jQuery(option.itemListQuery).append(newNode)
                items = jQuery(option.itemsQuery)
             }

            if (iconItemClickHdlr != null && item != null) {
                val item0 = items.eq(index)
                item0.on("click", iconItemClickHdlr!!)
                if (selected) {
                    item0.addClass("selected")
                }
            }
        }
    }
    /**
     * remove item from icon list
     */
    fun removeItem(index: Int) {
        val items = jQuery(option.itemsQuery)
        if (iconItemClickHdlr != null) {
            items.eq(index).off("click", iconItemClickHdlr!!) 
        }
        items.eq(index).remove()
    }

    /**
     * handle item click
     */
    fun onIconItemClick(e: JQueryEventObject, args: Any?) {
        postSelectIconNode(e.delegateTarget)
    }

    /**
     * select icon node lately
     */
    fun postSelectIconNode(iconNode: Element) {
        window.setTimeout({
            selectIconNode(iconNode)
        }, 100)
    }

    /**
     * select icon node
     */
    fun selectIconNode(iconNode: Element) {
        val iconNodeJ = jQuery(iconNode)
        iconNodeJ.siblings().removeClass("selected")
        iconNodeJ.addClass("selected")
        val iconDisplayingNode = jQuery(option.rtIconItemQuery, 
            iconNodeJ as JQuery?) 
        val persisIcon = createIconIdFromNode(iconDisplayingNode)
        if (persisIcon != null) {
            selectedMineIcon = persisIcon!!
        }
    }

    /**
     * creawte persistence icon from node
     */
    fun createIconIdFromNode(node : JQuery): Persistence.Icon? {
        val prefix = node.data("prefix")
        val iconName = node.data("icon")
        var result: Persistence.Icon? = null
        if (prefix != null && iconName != null) {
            result = Persistence.Icon(prefix as String, iconName as String)
        }
        return result
    }
}
