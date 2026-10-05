package com.yjh.base.core.db

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/**
 * 通用数据库基础操作接口
 * Created by youjiahui on 2026/10/4
 */
interface BaseDao<T> {

    // 插入单条，如果主键冲突直接替换旧数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: T): Long

    // 批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<T>): LongArray

    // 删除单条
    @Delete
    fun delete(item: T): Int

    // 批量删除
    @Delete
    fun delete(items: List<T>): Int

    // 更新单条
    @Update
    fun update(item: T): Int

    // 批量更新
    @Update
    fun update(items: List<T>): Int

}