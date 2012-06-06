/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
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

package org.jongo;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;
import org.jongo.marshall.Marshaller;
import org.jongo.marshall.Unmarshaller;
import org.jongo.query.Query;
import org.jongo.query.QueryFactory;

import java.util.Iterator;
import java.util.List;

import static org.jongo.ResultMapperFactory.newMapper;


class DefaultMongoCollection implements MongoCollection {

    private final DBCollection collection;
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;
    private final QueryFactory queryFactory;

    DefaultMongoCollection(DBCollection dbCollection, Marshaller marshaller, Unmarshaller unmarshaller) {
        this.collection = dbCollection;
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
        this.queryFactory = new QueryFactory();
    }

    public FindOne findOne(ObjectId id) {
        if (id == null) {
            throw new IllegalArgumentException("Object id must not be null");
        }
        return new FindOne(collection, unmarshaller, "{_id:#}", id);
    }

    public FindOne findOne(String query) {
        return new FindOne(collection, unmarshaller, query);
    }

    public FindOne findOne(String query, Object... parameters) {
        return new FindOne(collection, unmarshaller, query, parameters);
    }

    public Find find(String query) {
        return new Find(collection, unmarshaller, query);
    }

    public Find find(String query, Object... parameters) {
        return new Find(collection, unmarshaller, query, parameters);
    }

    public long count() {
        return collection.count();
    }

    public long count(String query) {
        return collection.count(createQuery(query).toDBObject());
    }

    public long count(String query, Object... parameters) {
        DBObject dbQuery = createQuery(query, parameters).toDBObject();
        return collection.count(dbQuery);
    }

    public WriteResult update(String query, String modifier) {
        return new Update(collection, query).multi().with(modifier);
    }

    public WriteResult update(String query, String modifier, WriteConcern concern) {
        return new Update(collection, query).multi().concern(concern).with(modifier);
    }

    public WriteResult upsert(String query, String modifier) {
        return new Update(collection, query).upsert().with(modifier);
    }

    public WriteResult upsert(String query, String modifier, WriteConcern concern) {
        return new Update(collection, query).upsert().concern(concern).with(modifier);
    }

    public <D> String save(D document) {
        return new Save(collection, marshaller, document).execute();
    }

    public <D> String save(D document, WriteConcern concern) {
        return new Save(collection, marshaller, document).concern(concern).execute();
    }

    public WriteResult insert(String query) {
        return insert(query, new Object[0]);
    }

    public WriteResult insert(String query, Object... parameters) {
        DBObject dbQuery = createQuery(query, parameters).toDBObject();
        return collection.save(dbQuery);
    }

    public WriteResult remove(String query) {
        return remove(query, new Object[0]);
    }

    public WriteResult remove(String query, Object... parameters) {
        return collection.remove(createQuery(query, parameters).toDBObject());
    }

    public WriteResult remove(ObjectId id) {
        return remove("{_id:#}", id);
    }

    @SuppressWarnings("unchecked")
    public <T> Iterable<T> distinct(String key, String query, final Class<T> clazz) {
        DBObject ref = createQuery(query).toDBObject();
        final List<?> distinct = collection.distinct(key, ref);
        if (BSONPrimitives.contains(clazz))
            return new Iterable<T>() {
                public Iterator<T> iterator() {
                    return (Iterator<T>) distinct.iterator();
                }
            };
        else
            return new MongoIterator<T>((Iterator<DBObject>) distinct.iterator(), newMapper(clazz, unmarshaller));
    }

    public void drop() {
        collection.drop();
    }

    public void ensureIndex(String index) {
        DBObject dbIndex = createQuery(index).toDBObject();
        collection.ensureIndex(dbIndex);
    }

    public String getName() {
        return collection.getName();
    }

    public DBCollection getDBCollection() {
        return collection;
    }

    private Query createQuery(String query, Object... parameters) {
        return queryFactory.createQuery(query, parameters);
    }

    private DBObject convertToJson(String json) {
        try {
            return ((DBObject) JSON.parse(json));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to save document, marshalled json cannot be parsed: " + json, e);
        }
    }
}
