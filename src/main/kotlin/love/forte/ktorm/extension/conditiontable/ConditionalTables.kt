package love.forte.ktorm.extension.conditiontable

import org.ktorm.dsl.Query
import org.ktorm.dsl.where
import org.ktorm.entity.Entity
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.filter
import org.ktorm.schema.ColumnDeclaring


/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined by each field in [ConditionalTable].
 *
 * ```kotlin
 * database.departments
 *     .filterBy(entity)
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * or
 *
 * ```kotlin
 * database.departments
 *     .filterBy(entity) { table, condition ->
 *         condition and (table.location eq LocationWrapper("GuangZhou"))
 *     }
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * If you want to handle the case when the condition may be null, you can refer to [filterByOr].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see filterByOr
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> EntitySequence<E, T>.filterBy(
    conditionEntity: E,
    andThen: ((table: T, condition: ColumnDeclaring<Boolean>) -> ColumnDeclaring<Boolean>) = { _, condition -> condition },
): EntitySequence<E, T> {
    return sourceTable.asCondition(conditionEntity)?.let { condition -> filter { table -> andThen(table, condition) } }
        ?: this
    
}

/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined in each field of [table] of type [ConditionalTable].
 *
 * ```kotlin
 * database.from(Departments)
 *     .select()
 *     .whereBy(Departments, entity)
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * or
 *
 * ```kotlin
 * database.from(DepartmentsWithCondition)
 *     .select()
 *     .whereBy(DepartmentsWithCondition, entity) {
 *         it and (Departments.location eq LocationWrapper("GuangZhou"))
 *     }
 *     .forEach { println(it) }
 * ```
 *
 * If you want to handle the case when the condition may be null, you can refer to [whereByOr].
 *
 * @param andThen When the condition exists, you can operate on it.
 * @see ConditionalTable
 * @see whereByOr
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> Query.whereBy(
    table: T, conditionEntity: E, andThen: ((condition: ColumnDeclaring<Boolean>) -> ColumnDeclaring<Boolean>) = { it },
): Query {
    return table.asCondition(conditionEntity)?.let { this.where(andThen(it)) } ?: this
}


/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined by each field in [ConditionalTable].
 *
 *
 * ```kotlin
 * database.departments
 *     .filterByOr(entity) { table, condition ->
 *         condition?.and(table.location eq LocationWrapper("GuangZhou")) // nullable.
 *     }
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * If you only care about the presence of conditions, you can refer to [filterBy].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see filterBy
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> EntitySequence<E, T>.filterByOr(
    conditionEntity: E,
    andThen: ((table: T, condition: ColumnDeclaring<Boolean>?) -> ColumnDeclaring<Boolean>?) = { _, condition -> condition },
): EntitySequence<E, T> {
    return andThen(sourceTable, sourceTable.asCondition(conditionEntity))?.let { condition -> filter { condition } }
        ?: this
    
}

/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined in each field of [table] of type [ConditionalTable].
 *
 * ```kotlin
 * database.from(DepartmentsWithCondition)
 *     .select()
 *     .filterByOr(DepartmentsWithCondition, entity) {
 *          // nullable.
 *         it?.and(DepartmentsWithCondition.location eq LocationWrapper("GuangZhou"))
 *     }
 *     .forEach { println(it) }
 * ```
 * If you only care about the presence of conditions, you can refer to [whereBy].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see whereBy
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> Query.whereByOr(
    table: T,
    conditionEntity: E,
    andThen: ((condition: ColumnDeclaring<Boolean>?) -> ColumnDeclaring<Boolean>?) = { it },
): Query {
    return andThen(table.asCondition(conditionEntity))?.let { this.where(it) } ?: this
}

