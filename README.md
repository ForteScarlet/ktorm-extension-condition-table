# [Ktorm][ktorm-home] Extension: Condition Table

作为 [**Ktorm**][ktorm-home] 的一个简易扩展库，
为 [**Ktorm**][ktorm-home] 提供简易扩展类 `ConditionTable`，
提供支持通过实体类进行查询的api。

> ⚠️ 仍在建设中

## 快速开始
此扩展库发布于Maven中央仓库，你可以使用 **Maven** 或 **Gradle** 等任何支持的手段来使用本扩展。

**Maven**
```xml
<!-- 环境中必须存在 ktorm 依赖 -->
<dependency>
    <groupId>org.ktorm</groupId>
    <artifactId>ktorm-core</artifactId>
    <version>${ktorm.version}</version>
</dependency>

<dependency>
    <groupId>love.forte.ktorm-extension</groupId>
    <artifactId>ktorm-extension-condition-table</artifactId>
    <!-- TODO -->
    <version>${ktorm-condition-table.version}</version>
</dependency>
```

**Gradle Groovy**
```groovy
// 环境中必须存在 ktorm 依赖
compile "org.ktorm:ktorm-core:${ktorm.version}"
compile "love.forte.ktorm-extension:ktorm-extension-condition-table:${ktorm-condition-table.version}"
```

**Gradle Kotlin DSL**
```kotlin
// 环境中必须存在 ktorm 依赖
implementation("org.ktorm:ktorm-core:${ktorm.version}")
implementation("love.forte.ktorm-extension:ktorm-extension-condition-table:${ktorm-condition-table.version}")
```

首先，创建用于映射数据库表结构的 **Kotlin Object**。先看看在普通的 `ktorm-core` 中是如何使用的。
在定义表结构映射之前，先定义实体类（接口）：
```kotlin
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()
    val id: Int
    var name: String
    var location: String
}

interface Employee : Entity<Employee> {
    companion object : Entity.Factory<Employee>()
    val id: Int
    var name: String
    var job: String
    var manager: Employee?
    var hireDate: LocalDate
    var salary: Long
    var department: Department
}
```

然后使用 kotlin object 实现 `Table<E>` 来提供对数据库表结构的映射： 

```kotlin
object Departments : Table<Department>("t_department") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val location = varchar("location").bindTo { it.location }
}

object Employees : Table<Employee>("t_employee") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val job = varchar("job").bindTo { it.job }
    val managerId = int("manager_id").bindTo { it.manager.id }
    val hireDate = date("hire_date").bindTo { it.hireDate }
    val salary = long("salary").bindTo { it.salary }
    val departmentId = int("department_id").references(Departments) { it.department }
}
```
> 摘自 [ktorm][ktorm-home] README.md

那么，如果希望拥有一个支持实体类查询API的表结构，那么你需要这样定义你的映射对象：
```kotlin
object Departments : ConditionalTable<Department>("t_department") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val location = varchar("location").bindTo { it.location }
}

object Employees : ConditionalTable<Employee>("t_employee") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val job = varchar("job").bindTo { it.job }
    val managerId = int("manager_id").bindTo { it.manager.id }
    val hireDate = date("hire_date").bindTo { it.hireDate }
    val salary = long("salary").bindTo { it.salary }
    val departmentId = int("department_id").references(Departments) { it.department }
}
```

大致的变化就是将 **`Table<E>`** 变化为 **`ConditionalTable<E>`**。
**`ConditionalTable<E>`** 扩展了 **`Table<E>`**，为字段提供了定义作为实体查询时的条件函数，如下示例：
```kotlin
object Departments : ConditionalTable<Department>("t_department") {
    val id = int("id").primaryKey().bindTo { it.id }.conditionOn { column, value -> // this: Department
        if (value != null) column eq value else column eq 1
    }
    
    val name = varchar("name").bindTo { it.name }.conditionNotNullOn { column, value -> // this: Department
        column like "%$value%"
    }
    
    val location = varchar("location").bindTo { it.location } // No conditions will be generated for this field(column)
}
```

- 对于 `id` 字段，使用了 `conditionOn { ... }`。当使用实体类查询的时候，
会自动向条件中填充 `if (value != null) column eq value else column eq 1` 的结果，
即如果 `id` 不为null，则SQL中相当于 `value = ${id}`；
如果 `id` 为null，则SQL中相当于 `value = 1`。

<br/>

- 对于 `name` 字段，使用了 `conditionNotNullOn { ... }`。当使用实体类查询的时候，只有在实体类中的 `name` 不为null的时候，
自动向条件中填充 `column like "%$value%"` 的结果，
即如果 `name` 不为null，则SQL中(类似的)相当于 `name LIKE CONCAT('%', ${name}, '%')`；
如果 `name` 为null，则不生成。

<br/>

- 对于 `location` 字段，由于没有定义条件，因此不会产生条件。

> `conditionOn { ... }` 和 `conditionNotNullOn { ... }` 理论上是可以重复使用的，
> 多次调用后，查询时会产生两次条件的并集。（ condition1 and condition2 ）
> 
> 但是一般情况下，为了更好的控制，还是建议只调用一次。

当需要使用查询的时候，通过 `EntitySequence` 或 `Query` 的扩展完成：

**定义：**
```kotlin
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()
    
    val id: Int
    var name: String
    var location: LocationWrapper
    var mixedCase: String?
}

object Departments : ConditionalTable<Department>("t_department") {
    companion object : Departments(null)
    
    override fun aliased(alias: String) = Departments(alias)
    
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name")
        .bindTo { it.name }
        .conditionNotNullOn { c, v ->
            c like "$v%"
        }
    val location = varchar("location").transform({ LocationWrapper(it) }, { it.underlying }).bindTo { it.location }
    val mixedCase = varchar("mixedCase").bindTo { it.mixedCase }
}

val entity = Department {
    name = "te"
}

val Database.departments get() = this.sequenceOf(Departments)

```

**使用：**
```kotlin
database.departments.filterBy(entity).forEach {
    // ...
}

database.from(Departments).select().whereBy(Departments, entity).forEach {
    // ...
}

database.departments.filterByOr(entity) { table, condition ->
    val extraCondition = table.location eq locationWrapper
    condition?.and(extraCondition) ?: extraCondition
}.forEach {
    // ...
}

database.from(Departments).select().whereByOr(Departments, entity) {
    it?.and(DepartmentsWithCondition.location eq locationWrapper) // or null.
}.forEach {
    // ...
}
```

你可以看到主要涉及的扩展就是如下几个函数：
- **`EntitySequence<E, T>.filterBy`**
- **`Query.whereBy`**
- **`EntitySequence<E, T>.filterByOr`**
- **`Query.whereByOr`**

它们分别对应了原本的 `EntitySequence<E, T>.filter` 和 `Query.where`。

在这其中，以 `By` 结尾的函数和 `ByOr` 结构的函数之间的主要区别在于它们的最后一个可选的 `andThen` 函数。

以下述代码为例：

```kotlin
// (1)
database.departments.filterBy(entity) { table, condition -> // condition: notnull
    condition and (table.location eq locationWrapper) // return: notnull
}.forEach {
    // ...
}

// (2)
database.departments.filterByOr(entity) { table, condition -> // condition: nullable
    condition?.and(table.location eq locationWrapper) // return: nullable
}.forEach {
    // ...
}
```

可以看出，以 `By` 结尾的函数中，最后的额外条件函数的参数 `condition` 和返回值 **不可为null** ，而 `ByOr` 接口的函数**允许它们为null**。




[ktorm-home]: https://github.com/kotlin-orm/ktorm
