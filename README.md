# AppRoomProduto: Cadastro de Produtos com Room SQLite

**Aluno(a):** Victor Hugo Soares Matos Brandão  
**RA:** 240653  
**Aluno(a):** Thales de Melo Fontes  
**RA:** 243925  
**Disciplina:** Programação para Dispositivos Móveis  
**Professor:** Alessandro Ferreira

## Sobre o projeto

Aplicativo Android em Kotlin que cadastra, lista, busca, atualiza e exclui produtos
usando persistência local com Room (SQLite). Os dados continuam salvos mesmo depois de
fechar o app. Além do tutorial base, foram implementados os 8 itens do complemento prático
(Parte A), incluindo os desafios extras de `RecyclerView` (A3) e exclusão por id com `@Delete` (A6).

## Tecnologias

- Kotlin
- Room 2.6.1 (SQLite)
- ViewBinding
- Corrotinas (`lifecycleScope`)
- RecyclerView
- Android Studio, Minimum SDK API 24

## Estrutura do projeto

```
AppRoomProduto/
└── app/src/main/
    ├── java/br/universidade/approomproduto/
    │   ├── model/Product.kt          ← tabela (Entidade)
    │   ├── dao/ProductDao.kt         ← comandos SQL (DAO)
    │   ├── db/AppDatabase.kt         ← o banco de dados
    │   └── view/
    │       ├── MainActivity.kt       ← a tela
    │       ├── ProductAdapter.kt     ← adapter do RecyclerView
    │       └── Formatters.kt         ← formatação de moeda (R$)
    ├── res/layout/activity_main.xml, item_product.xml
    └── AndroidManifest.xml
```

## Prints do app

![Tela inicial](docs/prints/tela-inicial.png)
![Logcat com a listagem](docs/prints/logcat.png)

## Parte A: Complemento prático

### A1. Limpar campos após inserir

**O que foi feito:** depois que o produto é gravado com sucesso no banco, os campos Nome, Preço,
Quantidade e Descrição são apagados com `text.clear()` e o cursor volta para o campo Nome com
`requestFocus()`, deixando o app pronto para o próximo cadastro.

Trecho de código (`MainActivity.kt`):

```kotlin
lifecycleScope.launch {
    productDao.insertAll(product)
    Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

    // A1 - limpa os campos e devolve o cursor para o campo Nome
    binding.editProductName.text.clear()
    binding.editProductPrice.text.clear()
    binding.editProductQuantity.text.clear()
    binding.editProductDescription.text.clear()
    binding.editProductName.requestFocus()
}
```

Print: ![A1](docs/prints/a1.png)

### A2. Validação de preço

**O que foi feito:** a função `lerPreco()` troca vírgula por ponto, converte o texto em número e
só devolve o valor se ele for maior que zero. Se o preço estiver vazio, for zero, negativo ou
inválido, o erro aparece no próprio campo (`error`) e o produto não é salvo. O nome vazio também
é validado da mesma forma.

Trecho de código (`MainActivity.kt`):

```kotlin
private fun lerPreco(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

// dentro de inserirProduto():
if (price == null) {
    binding.editProductPrice.error = "Informe um preço válido"
    valido = false
}
if (!valido || price == null) {
    Toast.makeText(this, "Preencha os campos obrigatórios!", Toast.LENGTH_SHORT).show()
    return
}
```

Print: ![A2](docs/prints/a2.png)

### A3. Lista na tela (com RecyclerView)

**O que foi feito:** ao clicar em Listar Produtos, os produtos aparecem na própria tela, abaixo dos
botões, em um `RecyclerView` (desafio extra). Cada item mostra id, nome, preço, quantidade e
descrição, e tem um botão Excluir. A listagem no Logcat do tutorial foi mantida.

Trecho de código (`MainActivity.kt` e `ProductAdapter.kt`):

```kotlin
// MainActivity.kt
adapter = ProductAdapter { product -> excluirProduto(product) }
binding.recyclerProdutos.layoutManager = LinearLayoutManager(this)
binding.recyclerProdutos.adapter = adapter

private fun listarProdutos() {
    lifecycleScope.launch {
        val products = carregarLista() // busca no banco e atualiza o RecyclerView
        products.forEach { Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}") }
    }
}

// ProductAdapter.kt
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val product = items[position]
    holder.binding.txtProduto.text =
        "#${product.id} - ${product.name}\n" +
            "${formatarMoeda(product.price)} | Qtd: ${product.quantity}\n" +
            (product.description ?: "sem descrição")
    holder.binding.btnExcluirItem.setOnClickListener { onExcluir(product) }
}
```

Print: ![A3](docs/prints/a3.png)

### A4. Total do estoque

**O que foi feito:** junto da lista, um `TextView` mostra a quantidade de produtos cadastrados
(`products.size`) e a soma dos preços (`products.sumOf { it.price }`), formatada em reais.
O resumo é atualizado sempre que a lista é recarregada (listar, buscar, excluir ou atualizar).

Trecho de código (`MainActivity.kt`):

```kotlin
private fun mostrarProdutos(products: List<Product>) {
    adapter.setProducts(products)
    val soma = products.sumOf { it.price }
    binding.txtResumo.text =
        "Produtos cadastrados: ${products.size}\nSoma dos preços: ${formatarMoeda(soma)}"
}
```

Print: ![A4](docs/prints/a4.png)

### A5. Buscar por nome

**O que foi feito:** foi criada no DAO a consulta `buscarPorNome` com `LIKE`. Na tela há um campo
de busca e o botão Buscar. O texto digitado é envolvido por `%` (`"%$texto%"`), então digitar
"tec" encontra "Teclado". Se o campo estiver vazio, a busca mostra todos os produtos.

Trecho de código:

```kotlin
// ProductDao.kt
@Query("SELECT * FROM product WHERE name LIKE :nome")
suspend fun buscarPorNome(nome: String): List<Product>

// MainActivity.kt
val products = if (texto.isEmpty()) {
    productDao.getAll()
} else {
    productDao.buscarPorNome("%$texto%") // % encontra nomes parecidos
}
mostrarProdutos(products)
```

Print: ![A5](docs/prints/a5.png)

### A6. Excluir produtos

**O que foi feito:** o DAO ganhou `deleteAll()` (com `@Query("DELETE FROM product")`) e `delete()`
(com `@Delete`, desafio extra). O botão Excluir Todos pede confirmação em um `AlertDialog` antes
de apagar tudo, e cada item da lista tem seu próprio botão Excluir, que remove só aquele produto.

Trecho de código:

```kotlin
// ProductDao.kt
@Query("DELETE FROM product")
suspend fun deleteAll()

@Delete
suspend fun delete(product: Product)

// MainActivity.kt
.setPositiveButton("Excluir") { _, _ ->
    lifecycleScope.launch {
        productDao.deleteAll()
        carregarLista()
    }
}

private fun excluirProduto(product: Product) {
    lifecycleScope.launch {
        productDao.delete(product)
        carregarLista()
    }
}
```

Print: ![A6](docs/prints/a6.png)

### A7. Atualizar produto

**O que foi feito:** o DAO ganhou `@Update` e `getById()`. Na tela, o usuário informa o id do
produto (visível na lista) e o novo preço. O app busca o produto, cria uma cópia com o preço novo
(`copy(price = novoPreco)`) e a atualiza no banco. Se o id não existir, aparece uma mensagem; o
novo preço passa pela mesma validação do A2.

Trecho de código:

```kotlin
// ProductDao.kt
@Update
suspend fun update(product: Product)

@Query("SELECT * FROM product WHERE id = :id")
suspend fun getById(id: Int): Product?

// MainActivity.kt
val produto = productDao.getById(id)
if (produto == null) {
    Toast.makeText(this@MainActivity, "Produto #$id não encontrado", Toast.LENGTH_SHORT).show()
} else {
    productDao.update(produto.copy(price = novoPreco))
    carregarLista()
}
```

Print: ![A7](docs/prints/a7.png)

### A8. Novo campo na tabela (quantity)

**O que foi feito:** a entidade `Product` ganhou o atributo `quantity: Int`, o layout ganhou o campo
"Quantidade em estoque" e o `version` do `@Database` passou de 1 para 2. A quantidade também
aparece na lista.

Trecho de código:

```kotlin
// Product.kt
@Entity
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "quantity") val quantity: Int = 0
)

// AppDatabase.kt
@Database(entities = [Product::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
```

**O que aconteceu com os produtos já cadastrados e por quê:** eles foram apagados. Ao abrir o app
com `version = 2`, o Room percebe que o banco que está no celular ainda é a versão 1 (tabela sem a
coluna `quantity`) e que o esquema esperado mudou. Como não foi escrita nenhuma `Migration` que
ensine a passar da versão 1 para a 2, entra em ação o `fallbackToDestructiveMigration()` que está
no `Room.databaseBuilder`: ele apaga as tabelas antigas e cria tudo de novo já com a coluna nova.
O resultado é um banco vazio. Se esse método não estivesse configurado, o app fecharia com um
`IllegalStateException` avisando que uma migração da versão 1 para a 2 era necessária e não foi
encontrada. Isso é aceitável durante o desenvolvimento, mas em um app real os dados dos usuários
seriam perdidos.

Print: ![A8](docs/prints/a8.png)

## Parte B: Questões dissertativas

### B1. O que é persistência de dados? O que aconteceria com os produtos se fossem guardados apenas em uma MutableList dentro da MainActivity?

**Resposta:** Persistência de dados é a capacidade de guardar informações de forma que elas
continuem existindo depois que o programa é fechado ou o celular é reiniciado, normalmente gravando
em um arquivo ou banco de dados no armazenamento do aparelho. No meu projeto, quando cadastro um
produto, ele é gravado no arquivo do banco SQLite pelo Room, e por isso consigo fechar o app,
abri-lo de novo, clicar em Listar Produtos e ver tudo lá. Se eu guardasse os produtos apenas em uma
`MutableList` dentro da `MainActivity`, eles ficariam só na memória RAM, que é temporária. A lista
seria perdida sempre que o app fosse fechado ou o sistema o encerrasse para liberar memória. Pior:
até uma simples rotação de tela recria a Activity e uma lista nova e vazia seria criada, apagando os
produtos que o usuário tinha cadastrado. Ou seja, uma lista em memória serve para guardar dados por
poucos instantes, mas não para um cadastro que precisa durar.

### B2. Qual é a diferença entre usar o SQLite diretamente e usar o Room? Cite pelo menos duas vantagens do Room.

**Resposta:** Com o SQLite direto, é preciso criar uma classe que herda de `SQLiteOpenHelper`, escrever
à mão os comandos `CREATE TABLE`, `INSERT`, `SELECT` etc. em texto, e percorrer o `Cursor` coluna por
coluna para transformar cada linha em um objeto. Qualquer erro de digitação em um comando SQL só
aparece quando o app está rodando. O Room é uma camada por cima do SQLite que faz grande parte desse
trabalho repetitivo. Uma primeira vantagem é a verificação em tempo de compilação: se eu escrever
errado a consulta `@Query("SELECT * FROM product")`, o erro aparece na hora de compilar, e não com o
app já na mão do usuário. Uma segunda vantagem é que o Room converte sozinho as linhas do banco em
objetos Kotlin (como a `Product`) e gera o código do `INSERT`, `UPDATE` e `DELETE` a partir de
anotações como `@Insert`, o que deixa o código bem menor e mais fácil de manter. Além disso, ele se
integra com corrotinas por meio do `suspend`, ajudando a não travar a tela.

### B3. Explique o papel de cada uma das três peças do Room: Entity, DAO e Database. Como elas se relacionam no seu projeto?

**Resposta:** A Entity é a classe que representa uma tabela do banco; no projeto é a `Product`,
marcada com `@Entity`, em que cada atributo (`name`, `price`, `description`, `quantity`) vira uma
coluna e o `id` é a chave primária gerada automaticamente. O DAO (Data Access Object) é a interface
em que ficam os comandos que posso executar sobre os dados; no projeto, o `ProductDao` tem as
funções de inserir, listar, buscar por nome, atualizar e excluir, cada uma ligada a uma anotação
como `@Insert`, `@Query`, `@Update` e `@Delete`. O Database é a classe `AppDatabase`, marcada com
`@Database`, que reúne a lista de entidades (`entities = [Product::class]`), define a versão do
banco e expõe o DAO por meio de `productDao()`. A relação entre elas é: o `AppDatabase` conhece a
entidade `Product` e entrega o `ProductDao`; na `MainActivity`, crio o banco com
`Room.databaseBuilder`, pego o DAO e uso as funções dele, e o Room trabalha com objetos `Product`
nos dois sentidos.

### B4. Por que o Android não permite acessar o banco na thread principal? O que é um ANR? Como suspend e lifecycleScope.launch resolvem esse problema?

**Resposta:** A thread principal (UI thread) é a responsável por desenhar a tela e responder aos
toques do usuário. Acessar o banco de dados é uma operação de disco, que pode demorar; se isso for
feito na thread principal, a tela fica congelada até a consulta terminar. Se o app ficar sem
responder por alguns segundos, o Android mostra o erro ANR (Application Not Responding), que oferece
ao usuário fechar o aplicativo. Por isso o Room bloqueia consultas na thread principal e lança o
erro "Cannot access database on the main thread". No meu projeto, as funções do DAO são `suspend`,
ou seja, podem ser pausadas e retomadas sem bloquear a thread em que foram chamadas, e o Room
executa o trabalho pesado em segundo plano. O `lifecycleScope.launch { }` abre a corrotina que chama
essas funções e ainda a vincula ao ciclo de vida da Activity: se a tela for fechada, a corrotina é
cancelada sozinha, evitando vazamento de memória ou tentativa de atualizar uma tela que não existe.

### B5. O que faz o fallbackToDestructiveMigration()? Por que ele é perigoso em um app publicado? O que seria uma Migration?

**Resposta:** O `fallbackToDestructiveMigration()` diz ao Room o que fazer quando a versão do banco
muda e não existe uma migração definida: apagar o banco antigo e criar um novo, vazio, com a
estrutura atual. Isso foi o que aconteceu no item A8, quando passei o `version` de 1 para 2 e os
produtos cadastrados sumiram. Em desenvolvimento isso é prático, porque evita ter que desinstalar o
app a cada mudança. Em um app publicado na Play Store é perigoso, porque, a cada atualização que
altere a tabela, todos os usuários perderiam os dados que cadastraram, sem aviso e sem como
recuperar. Uma `Migration` é um código que ensina o Room a converter o banco de uma versão para a
outra sem apagar nada. Por exemplo, para a mudança do A8 uma migração da versão 1 para a 2 executaria
`ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0`, mantendo os produtos antigos
e apenas acrescentando a coluna nova. Essa migração é passada ao banco com `.addMigrations(...)`.

### B6. Qual é a vantagem do ViewBinding em relação ao findViewById? Que tipo de erro ele ajuda a evitar?

**Resposta:** Com `findViewById` eu preciso informar o id do componente e, muitas vezes, o tipo dele,
a cada vez que for usá-lo. Isso traz riscos: se eu errar o id ou usar um id que não existe no layout
carregado, o app só acusa o problema na execução, e se eu tratar o componente como um tipo errado
(por exemplo, um `Button` como `EditText`) também ocorre um erro de execução. O ViewBinding gera
uma classe para cada layout (no projeto, a `ActivityMainBinding`) com uma propriedade para cada
componente que tenha id, já com o tipo certo, como `binding.editProductName`. Assim os erros
aparecem na hora de compilar, e não com o app rodando. Ele evita principalmente o `NullPointerException`
por id errado ou de outro layout e o `ClassCastException` por tipo incorreto, e ainda deixa o código
mais curto e legível, sem precisar procurar o componente toda hora.

### B7. Por que em apps maiores é recomendado criar o banco como Singleton? O que poderia acontecer se várias telas criassem cada uma o seu banco?

**Resposta:** No tutorial, o banco é criado dentro da `MainActivity` porque o app tem só uma tela.
Em um app maior, com várias telas, é recomendado ter uma única instância do banco para o app inteiro
(Singleton), normalmente criada uma vez e reutilizada por todas as telas. Criar uma instância do
banco é uma operação cara, pois abre conexão com o arquivo e monta toda a estrutura do Room; fazer isso
em cada tela gastaria memória e tempo à toa. O problema maior é que várias instâncias apontando para o
mesmo arquivo podem gerar inconsistências: cada instância tem seus próprios mecanismos internos e,
por exemplo, uma tela pode não ser avisada quando outra altera os dados, podendo mostrar informação
desatualizada, além de aumentar a chance de conflitos e de erros de acesso concorrente ao arquivo.
Com um único objeto compartilhado, todas as telas enxergam os mesmos dados e o Room consegue
coordenar as operações com segurança.

### B8. Reflexão pessoal: dificuldades, como resolveu, e um app real em que o Room seria útil.

**Resposta:** As principais dificuldades foram a configuração inicial do Gradle (lembrar de adicionar
o plugin `kotlin-kapt`, a dependência do `room-compiler` e ativar o `viewBinding = true`, já que sem
isso aparece o erro `Unresolved reference: ActivityMainBinding`) e entender por que as funções do DAO
precisam ser `suspend`, o que me fez estudar o funcionamento das corrotinas e do `lifecycleScope`.
Também precisei de atenção no item A8, porque ao mudar a versão do banco os dados antigos sumiram, e
só entendi o motivo ao estudar o `fallbackToDestructiveMigration()`. Resolvi esses problemas lendo as
mensagens de erro do Logcat, consultando a tabela de problemas comuns do tutorial e testando passo
a passo no emulador. Um app real em que a persistência local com Room seria útil é um app de controle
de estoque para uma pequena loja: o dono cadastraria os produtos com nome, preço e quantidade, daria
baixa quando vendesse e consultaria o que está acabando, mesmo sem internet. Eu aplicaria exatamente o
que foi feito aqui: uma entidade `Produto`, um DAO com buscar, atualizar a quantidade e excluir, e uma
migração para quando o app ganhar novos campos (como categoria ou fornecedor), sem perder o que a loja
já cadastrou.

## Como executar

1. Clone o repositório: `git clone https://github.com/Victor-dev-Hugo/AppRoomProduto.git`
2. Abra a pasta no Android Studio e aguarde o Gradle sincronizar
3. Execute em um emulador (API 24+) ou em um celular com depuração USB ativada
4. Para ver a listagem no Logcat, use o filtro `tag:TAG`
