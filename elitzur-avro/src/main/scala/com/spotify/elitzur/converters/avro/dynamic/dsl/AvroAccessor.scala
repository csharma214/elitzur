/*
 * Copyright 2021 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.spotify.elitzur.converters.avro.dynamic.dsl

import org.apache.avro.generic.GenericRecord

import java.{util => ju}

trait BaseAccessor {
  def fn: Any => Any
}

trait InnerBaseAccessor extends BaseAccessor {
  val innerOps: List[BaseAccessor]
  val innerFn: Any => Any = (o: Any) => FieldAccessor(innerOps).combineFns(o)
}

trait ArrayBaseAccessor extends InnerBaseAccessor

trait NullableBaseAccessor extends InnerBaseAccessor

case class NoopAccessor() extends BaseAccessor {
  def fn: Any => Any = (o: Any) => o
}

case class IndexAccessor(field: String) extends BaseAccessor {
  override def fn: Any => Any = (o: Any) => o.asInstanceOf[GenericRecord].get(field)
}

case class NullableAccessor(field: String, innerOps: List[BaseAccessor])
  extends NullableBaseAccessor {
  override def fn: Any => Any = (o: Any) => {
    val innerAvroObj = o.asInstanceOf[GenericRecord].get(field)
    if (innerAvroObj == null) null else innerFn(o)
  }
}

case class ArrayFlatmapAccessor(field: String,  innerOps: List[BaseAccessor])
  extends ArrayBaseAccessor {
  override def fn: Any => Any = (o: Any) => {
    val innerAvroObj = o.asInstanceOf[GenericRecord].get(field)
    val res = new ju.ArrayList[Any]
    innerAvroObj.asInstanceOf[ju.List[Any]].forEach(
      elem => innerFn(elem).asInstanceOf[ju.List[Any]].forEach(x => res.add(x))
    )
    res
  }
}

case class ArrayMapAccessor(field: String, innerOps: List[BaseAccessor])
  extends ArrayBaseAccessor {
  override def fn: Any => Any = (o: Any) => {
    val innerAvroObj = o.asInstanceOf[GenericRecord].get(field)
    val res = new ju.ArrayList[Any]
    innerAvroObj.asInstanceOf[ju.List[Any]].forEach(elem => res.add(innerFn(elem)))
    res
  }
}

case class ArrayNoopAccessor(field: String, innerOps: List[BaseAccessor], flatten: Boolean)
  extends ArrayBaseAccessor {
  override def fn: Any => Any = (o: Any) => IndexAccessor(field).fn(o)
}
