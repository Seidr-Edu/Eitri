Source: _PlantUML Language Reference Guide v1.2025.0 – Class Diagram_

---

# PlantUML Class Diagram — Agent Ruleset (Compact)

## 0. File Structure

```
@startuml [name]
  statements...
@enduml
```

---

## 1. Element Declarations

```
class X
abstract class X
interface X
enum X
annotation X
struct X
protocol X
entity X
exception X
metaclass X
circle X | () X
diamond X | <> X
```

**Alias**

```
class "Display Name" as Alias
```

> If alias is used → **always reference by alias**

---

## 2. Class Body (Fields / Methods)

```
class X {
  field
  method()
}
```

**Visibility**

```
+ public
- private
# protected
~ package
```

**Static / Abstract**

```
{static} field
{abstract} method()
{classifier} == {static}
```

**Force type**

```
{field} text
{method} text()
```

**Grouping separators**

```
--  ..  ==  __
```

---

## 3. Relations (Core)

```
Inheritance        A <|-- B
Implementation     A <|.. B
Composition        A *-- B
Aggregation        A o-- B
Association        A -- B
Dependency         A --> B
Weak dependency    A ..> B
```

**Dotted line**: replace `--` with `..`

---

## 4. Relation Labels & Cardinality

```
A "1" -- "many" B : label
A -- B : acts >
A -- B : < owns
```

---

## 5. Direction & Layout

```
A -left-> B
A -right-> B
A -up-> B
A -down-> B
```

Short forms: `-l- -r- -u- -d-`

Global:

```
left to right direction
```

---

## 6. Advanced Relations

**Association class**

```
(A, B) .. AssocClass
(A, B) .  AssocClass
```

**Qualified association**

```
A [Qualifier] -- B
```

**Self association**

```
X -- X
```

**Member-to-member**

```
A::field --> B::field
```

---

## 7. Lollipop Interface

```
bar ()- foo
foo -() bar
```

---

## 8. Packages / Namespaces

```
package P {
  class X
}
```

Nested allowed.

**Separator**

```
set separator ::
class A::B::C
```

Disable auto-packages:

```
set separator none
!pragma useIntermediatePackages false
```

---

## 9. Notes

**On elements**

```
note left of X : text
note right/top/bottom of X
```

**Floating**

```
note "text" as N
X .. N
```

**On members**

```
note right of X::field
note right of X::"method(sig)"
```

**On links**

```
note on link
note left/right/top/bottom on link
```

---

## 10. Stereotypes

```
class X <<S>>
package P <<Folder>>
```

Custom spot:

```
<< (C,#color) S >>
```

---

## 11. Hide / Show / Remove

```
hide empty members
hide fields | methods | members
hide private | protected | package
hide class | interface | enum
hide <<S>>
hide X
remove X
```

**Tags**

```
class X $tag
hide $tag
remove *
restore $tag
```

**Unlinked**

```
hide @unlinked
remove @unlinked
```

---

## 12. Generics

```
class X<T>
class X<? extends Y>
```

Disable:

```
skinparam genericDisplay old
```

---

## 13. Styling (Inline)

**Relations**

```
A -[#red,dashed,thickness=2]-> B
A --> B #line:red;line.bold;text:red
```

**Classes**

```
class X #back:color;line:color;text:color
class X #color ##[dashed]color
```

---

## 14. Skinparam (Global)

```
skinparam class {
  BackgroundColor color
  BorderColor color
  ArrowColor color
}
skinparam stereotypeCBackgroundColor<<S>> color
```

---

## 15. Layout Control

**Group**

```
together {
  class A
  class B
}
```

**Hidden links**

```
A -[hidden]-> B
```

**Inheritance grouping**

```
skinparam groupInheritance N
```

---

## 16. Diagram Orientation

Graphviz (default):

```
top to bottom direction
left to right direction
```

Smetana:

```
!pragma layout smetana
```

---

## 17. Page Splitting

```
page HxV
```

---

## 18. JSON Embedding

```
json X {
  "k": "v"
}
```

---

## 19. Extends / Implements Keywords

```
class A extends B, C
class A implements I
```

---

## 20. Naming Rules

- Non-letters → use quotes or alias
- Names starting with `$` **cannot be hidden/removed** unless aliased or tagged

---

### Agent Guidance (Strict)

- **Always prefer aliases** for complex names
- **Never mix alias + raw name**
- **Use minimal arrows**; rely on layout engine
- **Hide aggressively** for large diagrams
- **One concept per relation**

---
