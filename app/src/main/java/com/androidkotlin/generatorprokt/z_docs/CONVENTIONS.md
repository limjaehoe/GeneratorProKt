# GeneratorProKt 코딩 컨벤션

이 문서는 GeneratorProKt 프로젝트의 코딩 표준과 컨벤션을 정의합니다. 일관된 코드 스타일은 가독성을 높이고 유지보수를 용이하게 합니다.

## 일반 컨벤션

### 파일 네이밍
- 클래스 이름과 파일 이름이 일치해야 합니다.
- 파일 이름은 PascalCase를 사용합니다.
- 인터페이스의 경우 이름 앞에 'I'를 붙이지 않습니다.
- 확장 함수가 포함된 파일은 `{대상클래스}Extensions.kt` 형식으로 명명합니다.

예시:
```
UserRepository.kt
StringExtensions.kt
MainActivity.kt
```

### 패키지 구조
- 패키지 이름은 소문자만 사용합니다.
- 패키지 경로는 기능이나 레이어를 기준으로 구성합니다.

예시:
```
com.yourcompany.generatorprokt.data.device.usb
com.yourcompany.generatorprokt.domain.usecase.apr
```

## 코틀린 스타일 가이드

### 클래스 레이아웃
클래스 내부 순서:
1. 프로퍼티 선언과 초기화 블록
2. 보조 생성자
3. 메서드 선언
4. 컴패니언 객체
5. 중첩 또는 내부 클래스

### 네이밍 규칙

- **클래스 및 객체**: PascalCase 사용
  ```kotlin
  class XrayDevice
  object Constants
  ```

- **함수 및 프로퍼티**: camelCase 사용
  ```kotlin
  fun connectToDevice()
  val deviceStatus
  ```

- **상수**: SCREAMING_SNAKE_CASE 사용
  ```kotlin
  const val MAX_RETRY_COUNT = 3
  ```

- **열거형(Enum)**: 각 값은 SCREAMING_SNAKE_CASE 사용
  ```kotlin
  enum class ConnectionState {
      CONNECTED,
      DISCONNECTED,
      CONNECTING
  }
  ```

### 문법 규칙

- 함수 매개변수가 한 줄에 맞지 않는 경우 들여쓰기를 사용합니다.
  ```kotlin
  fun createDevice(
      name: String,
      serialNumber: String,
      firmwareVersion: String,
      connectionType: ConnectionType
  ): Device { ... }
  ```

- 람다식은 가능한 한 간결하게 작성합니다.
  ```kotlin
  items.filter { it.isActive }
  ```

- 람다가 한 줄을 초과하면 중괄호 다음에 새 줄을 시작합니다.
  ```kotlin
  items.filter { item ->
      val result = item.isActive
      result && item.isAvailable
  }
  ```

## MVVM 패턴 컨벤션

### ViewModel
- ViewModel 클래스 이름은 `{기능}ViewModel` 형식을 따릅니다.
- UI 상태는 StateFlow 또는 LiveData로 노출합니다.
- 이벤트는 SharedFlow 또는 SingleLiveEvent로 처리합니다.

예시:
```kotlin
class AprViewModel @Inject constructor(
    private val getAprsUseCase: GetAprsUseCase
) : ViewModel() {
    private val _aprs = MutableStateFlow<List<Apr>>(emptyList())
    val aprs: StateFlow<List<Apr>> = _aprs.asStateFlow()
    
    // ...
}
```

### Fragment/Activity
- UI 관련 로직만 포함해야 합니다.
- 모든 비즈니스 로직은 ViewModel에 위임해야 합니다.

## 의존성 주입 컨벤션

### Hilt 모듈
- Hilt 모듈 클래스 이름은 `{기능}Module` 형식을 사용합니다.
- 각 모듈은 특정 레이어 또는 기능에 중점을 둡니다.

예시:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // ...
}
```

## 코루틴 및 Flow 컨벤션

- 코루틴 스코프는 명확한 목적에 따라 사용합니다.
- ViewModel에서는 viewModelScope를 사용합니다.
- 장기 실행 작업에는 Dispatchers.IO를 사용합니다.
- UI 관련 작업에는 Dispatchers.Main을 사용합니다.

예시:
```kotlin
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        repository.fetchData()
    }
    _data.value = result
}
```

## Room DB 컨벤션

### 엔티티
- 엔티티 클래스 이름은 DB 테이블을 나타내는 의미적 이름을 사용합니다.
- 테이블 이름은 snake_case를 사용합니다.

예시:
```kotlin
@Entity(tableName = "apr_settings")
data class AprSettingEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val value: Double
)
```

### DAO
- DAO 인터페이스 이름은 `{엔티티}Dao` 형식을 사용합니다.
- 쿼리 메서드 이름은 해당 작업을 명확하게 설명해야 합니다.

예시:
```kotlin
@Dao
interface AprSettingDao {
    @Query("SELECT * FROM apr_settings")
    fun getAllSettings(): Flow<List<AprSettingEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AprSettingEntity)
}
```

## 테스트 컨벤션

- 테스트 클래스 이름은 `{테스트대상클래스}Test` 형식을 사용합니다.
- 테스트 메서드 이름은 `should{결과}_when{조건}` 형식을 사용합니다.

예시:
```kotlin
class UserRepositoryTest {
    @Test
    fun shouldReturnUser_whenUserExists() {
        // ...
    }
}
```

## 리소스 컨벤션

### 레이아웃 파일
- 레이아웃 파일 이름은 `{컴포넌트타입}_{설명}.xml` 형식을 사용합니다.
    - activity_main.xml
    - fragment_settings.xml
    - item_apr.xml
    - dialog_confirmation.xml

### 문자열 리소스
- 문자열 리소스 ID는 `{기능}_{설명}` 형식을 사용합니다.
    - apr_title
    - settings_save_button
    - error_connection_failed

## 린트 및 정적 분석

프로젝트는 다음 정적 분석 도구를 사용합니다:
- ktlint: 코틀린 코드 스타일 검사
- Detekt: 코드 품질 및 복잡성 분석

각 도구의 설정 방법은 [SETUP.md](SETUP.md)를 참조하세요.