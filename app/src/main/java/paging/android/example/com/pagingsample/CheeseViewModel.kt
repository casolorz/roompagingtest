/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package paging.android.example.com.pagingsample

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.paging.Config
import androidx.paging.toLiveData
import java.util.concurrent.Executors

/**
 * A simple ViewModel that provides a paged list of delicious Cheeses.
 */
class CheeseViewModel(app: Application) : AndroidViewModel(app) {
    private val TAG: String = "CheeseViewModel"
    val db = CheeseDb.get(app)
    val dao = db.cheeseDao()

    /**
     * We use -ktx Kotlin extension functions here, otherwise you would use LivePagedListBuilder(),
     * and PagedList.Config.Builder()
     */
    val allCheeses = dao.allCheesesByName().toLiveData(Config(
            /**
             * A good page size is a value that fills at least a screen worth of content on a large
             * device so the User is unlikely to see a null item.
             * You can play with this constant to observe the paging behavior.
             * <p>
             * It's possible to vary this with list device size, but often unnecessary, unless a
             * user scrolling on a large device is expected to scroll through items more quickly
             * than a small device, such as when the large device uses a grid layout of items.
             */
            pageSize = 30,

            /**
             * If placeholders are enabled, PagedList will report the full size but some items might
             * be null in onBind method (PagedListAdapter triggers a rebind when data is loaded).
             * <p>
             * If placeholders are disabled, onBind will never receive null but as more pages are
             * loaded, the scrollbars will jitter as new pages are loaded. You should probably
             * disable scrollbars if you disable placeholders.
             */
            enablePlaceholders = true,

            /**
             * Maximum number of items a PagedList should hold in memory at once.
             * <p>
             * This number triggers the PagedList to start dropping distant pages as more are loaded.
             */
            maxSize = 200))

    fun insert(text: CharSequence) = ioThread {
        val highestCheesePosition = dao.getHighestCheesePosition()
        dao.insert(Cheese(id = 0, name = text.toString(),position = highestCheesePosition+1))
    }

    fun remove(cheese: Cheese) = ioThread {
        dao.delete(cheese)
    }

    fun swap(pos1: Int, pos2: Int) {
        Executors.newSingleThreadExecutor().execute {

            db.runInTransaction() {
                val cheese1 = dao.getCheeseAt(pos1)
                val cheese2 = dao.getCheeseAt(pos2)
                if (cheese1 != null && cheese2 != null) {
                    Log.i(TAG, "Swapping " + cheese1.name + ":" + cheese1.position + " with  " + cheese2.name + ":" + cheese2.position)
                    val copy1 = cheese1.copy()
                    val copy2 = cheese2.copy()
                    copy1.position = cheese2.position
                    copy2.position = cheese1.position
                    Log.i(TAG, "Swapped " + copy1.name + ":" + copy1.position + " with  " + copy2.name + ":" + copy2.position)

                    dao.update(copy1, copy2)

                }
            }
        }
    }
}
