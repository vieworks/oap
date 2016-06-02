/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.etl.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 31.05.2016.
 */
public class Aggregator {
   public final String table;
   public final List<Aggregate> aggregates;
   public final Map<String, Aggregator> joins;


   public Aggregator(
      @JsonProperty( "table" ) String table,
      @JsonProperty( "aggregates" ) List<Aggregate> aggregates,
      @JsonProperty( "join" ) Map<String, Aggregator> joins ) {
      this.table = table;
      this.aggregates = aggregates;
      this.joins = joins;
   }

   public static class Aggregate {
      public final List<Accumulator> select;
      public final List<String> groupBy;
      public final String export;

      public Aggregate(
         @JsonProperty( "select" ) List<Accumulator> select,
         @JsonProperty( "group-by" ) List<String> groupBy,
         @JsonProperty( "export" ) String export ) {
         this.select = select;
         this.groupBy = groupBy;
         this.export = export;
      }
   }
}