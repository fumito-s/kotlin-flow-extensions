/*
 * Copyright 2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.kotlin.flow

import hu.akarnokd.kotlin.flow.impl.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

/**
 * Shares a single collector towards the upstream source and multicasts
 * values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.publish(transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
    FlowMulticastFunction(this, { PublishSubject() }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * cached values to any number of consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject() }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number of cached values to any number of
 * consumers which then can produce the output
 * flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxTime: Long, unit: TimeUnit, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxTime, unit) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number and up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, maxTime: Long, unit: TimeUnit, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize, maxTime, unit) }, transform)

/**
 * Shares a single collector towards the upstream source and multicasts
 * up to a given [maxSize] number and up to [maxTime] old cached values to any number of
 * consumers which then can produce the output flow of values.
 */
@FlowPreview
fun <T, R> Flow<T>.replay(maxSize: Int, maxTime: Long, unit: TimeUnit, timeSource: (TimeUnit) -> Long, transform: suspend (Flow<T>) -> Flow<R>) : Flow<R> =
        FlowMulticastFunction(this, { ReplaySubject(maxSize, maxTime, unit, timeSource) }, transform)

/**
 * Stats collecting the upstream on the specified dispatcher.
 */
@FlowPreview
fun <T> Flow<T>.startCollectOn(dispatcher: CoroutineDispatcher) : Flow<T> =
        FlowStartCollectOn(this, dispatcher)

/**
 * Emit values from the upstream followed by values from the other flow.
 */
fun <T> Flow<T>.concatWith(other: Flow<T>) : Flow<T> {
    val source = this
    return flow {
        source.collect {
            emit(it)
        }
        other.collect {
            emit(it)
        }
    }
}

/**
 * Consumes the upstream and dispatches individual items to a parrallel rail
 * of the parallel flow for further consumption.
 */
fun <T> Flow<T>.parallel(parallelism: Int, runOn: (Int) -> CoroutineDispatcher) : ParallelFlow<T> =
    FlowParallel(this, parallelism, runOn)

/**
 * Consumes the parallel upstream and turns it into a sequential flow again.
 */
@FlowPreview
fun <T> ParallelFlow<T>.sequential() : Flow<T> =
        FlowSequential(this)

/**
 * Maps the values of the upstream in parallel.
 */
fun <T, R> ParallelFlow<T>.map(mapper: suspend (T) -> R) : ParallelFlow<R> =
        FlowParallelMap(this, mapper)

/**
 * Filters the values of the upstream in parallel.
 */
fun <T> ParallelFlow<T>.filter(predicate: suspend (T) -> Boolean) : ParallelFlow<T> =
        FlowParallelFilter(this, predicate)

/**
 * Transform each upstream item into zero or more emits for the downstream
 * in parallel.
 */
fun <T, R> ParallelFlow<T>.transform(callback: suspend FlowCollector<R>.(T) -> Unit) : ParallelFlow<R> =
        FlowParallelTransform(this, callback)