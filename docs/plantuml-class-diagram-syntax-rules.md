# PlantUML Class Diagram Syntax — Ultra-Compact Patterns (Agent Context)

> Source: official PlantUML class diagram [docs](https://plantuml.com/class-diagram).

---

## 0) Wrapper

```
@startuml
' ...
@enduml
```

---

## 1) Declare elements (types)

```
class C
interface I
enum E
abstract class A
annotation Ann
entity Ent
exception Ex
struct S
protocol P
record R
dataclass D
metaclass M
stereotype ST
circle O         ' or: ()
diamond Dm       ' or: <>
```

---

## 2) Names, quotes, aliases, stereotypes, tags

```
class "Display Name"
class "Display Name" as Alias
class Alias as "Display Name"

class C <<Stereotype>>
class C $tag1 $tag2
```

---

## 3) Class body + members

### Inline member (uses `:`)

```
C : fieldName : Type
C : methodName(arg:Type) : Return
```

### Block body

```
class C {
  fieldName : Type
  methodName(arg:Type) : Return
}
```

### Force field/method parsing

```
C : {field} name()
C : {method} name
```

---

## 4) Visibility + escape

```
class C {
  -privateField
  #protectedField
  ~packageField
  +publicField
  -privateMethod()
  #protectedMethod()
  ~packageMethod()
  +publicMethod()
  \~literalTildeName()   ' escape leading ~ + - # if literal
}
```

### Disable attribute icons

```
skinparam classAttributeIconSize 0
```

---

## 5) Static / abstract member modifiers

```
class C {
  {static} COUNT : int
  {classifier} ID : String
  {abstract} run()
}
```

---

## 6) Section separators (member grouping)

```
class C {
  == Section ==
  --
  ..
  __
}
```

---

## 7) Relationships (arrows)

### Core relationship operators

```
A <|-- B   ' inheritance
A <|.. B   ' realization (implements)
A *-- B    ' composition
A o-- B    ' aggregation
A --> B    ' association / directed relation
A ..> B    ' dependency (dotted)
```

### Variants shown in docs (visual semantics)

```
A #-- B
A x-- B
A }-- B
A +-- B
A ^-- B
```

### Label

```
A --> B : label
```

### Cardinality (near ends)

```
A "1" --> "many" B
A "0..1" *-- "1..*" B : contains
```

### Directional arrows in label text

```
A --> B : <reads
A --> B : writes>
```

### Force layout direction on a link

```
A -left-> B
A -right-> B
A -up-> B
A -down-> B
```

### Horizontal links (short form)

```
A o- B
A *- B
```

### Link style options (example: thickness)

```
A -[thickness=2]-> B
```

---

## 8) Lollipop interfaces

```
bar ()- foo
bar ()-- foo
foo -() bar
```

---

## 9) Notes

### On elements

```
note left of C : text
note right of C : text
note top of C
  multiline
end note
note bottom of C
  multiline
end note
```

### Note attaches to last element (no “of X”)

```
note right : text
```

### Floating note + connect

```
note "free note" as N
C .. N
```

### Note on link

```
A --> B
note on link : text
A --> B
note right on link
  multiline
end note
```

### Note on fields/methods (left/right only; constraints apply)

```
class C {
  +m()
  note right of m : doc
}
```

### Formatting inside notes (examples)

```
note right of C : <b>bold</b> <i>italic</i> <u>underline</u>
note right of C : <color:red>colored</color> <size:18>big</size>
note right of C : <img:some.png>
```

---

## 10) Packages / namespaces

```
package "My Package" {
  class A
}
package "P" #DDDDDD {
  class B
}
```

### Package style

```
skinparam packageStyle rectangle
package P <<Folder>> { class C }
package Q <<Frame>>  { class D }
```

### Namespace separator control

```
set separator ::
set separator none
```

---

## 11) Layout directives

```
left to right direction
```

---

## 12) Generics display

```
class Foo<T>
skinparam genericDisplay old
```

---

## 13) Hide / show / remove / restore

### Hide members

```
hide empty members
hide members
hide fields
hide methods
hide circle
hide stereotype
```

### Hide by visibility

```
hide private members
hide protected members
hide package members
```

### Hide/remove classes

```
hide ClassName
remove ClassName
```

### Tags + wildcards + restore

```
remove *
restore ClassName
hide $tag
remove $tag
restore $tag
```

### Unlinked elements

```
hide @unlinked
remove @unlinked
```

---

## 14) Association to “same class” via diamond node

```
diamond Dm
C "1" -- Dm
Dm -- "many" C
```

---

## 15) Multi-page splitting

```
page 2x2
```

---

## 16) Group inheritance arrows

```
skinparam groupInheritance 2
```

---

### Copy-paste micro-template

```
@startuml
left to right direction

package app {
  interface I
  abstract class A
  class C <<S>> $core {
    +run()
    -helper()
    {static} COUNT : int
  }

  I <|.. C
  A <|-- C
}

note right of C : Example
@enduml
```
