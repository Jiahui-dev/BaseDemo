package com.yjh.base.core.db;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;
import java.util.List;

/**
 * 通用数据库基础操作接口
 * Created by jiahui on 2026/07/11
 */
public interface BaseDao<T> {

    //插入单挑，如果主键冲突直接替换旧数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(T item);

    //批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(List<T> items);

    //删除单条
    @Delete
    int delete(T item);

    //批量删除
    @Delete
    int delete(List<T> items);

    //更新单条
    @Update
    int update(T item);

    //批量更新
    @Update
    int update(List<T> items);
}
