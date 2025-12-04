현재 코드를 보면서 `fetchJoinUse()` 메소드에서 일반 조인과 페치 조인의 차이점을 설명해드리겠습니다.

## 일반 조인 (Inner Join)

코드에서 보면 일반 조인은 다음과 같이 사용됩니다:

```java
@Test
void join() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)  // 일반 조인
        .where(team.name.eq("teamA"))
        .fetch();
}
```


**일반 조인의 특징:**
- **데이터 필터링 목적**: 조인 조건에 맞는 데이터만 조회
- **지연 로딩 유지**: 연관된 엔티티는 실제 사용할 때까지 로딩되지 않음
- **N+1 문제 발생 가능**: 연관된 엔티티에 접근할 때마다 추가 쿼리 실행

## 페치 조인 (Fetch Join)

`fetchJoinUse()` 메소드에서 사용된 페치 조인:

```java
@Test
void fetchJoinUse(){
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()  // 페치 조인
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
}
```


**페치 조인의 특징:**
- **즉시 로딩**: 연관된 엔티티를 한 번의 쿼리로 함께 조회
- **N+1 문제 해결**: 추가 쿼리 없이 연관 데이터 접근 가능
- **성능 최적화**: 필요한 데이터를 미리 로딩하여 성능 향상

## 실제 차이점 비교

### 1. 쿼리 실행 횟수

**일반 조인 (`fetchJoinNo()`):**
```sql
-- 1. Member 조회 쿼리
SELECT * FROM member WHERE username = 'member1'
-- 2. Team 사용 시 추가 쿼리 (지연 로딩)
SELECT * FROM team WHERE id = ?
```


**페치 조인 (`fetchJoinUse()`):**
```sql
-- 한 번의 쿼리로 모든 데이터 조회
SELECT m.*, t.* FROM member m 
INNER JOIN team t ON m.team_id = t.id 
WHERE m.username = 'member1'
```


### 2. 로딩 상태 확인

코드에서 `emf.getPersistenceUnitUtil().isLoaded()`로 확인:

- **일반 조인**: `loaded = false` (지연 로딩 상태)
- **페치 조인**: `loaded = true` (즉시 로딩 완료)

### 3. 사용 시나리오

**일반 조인을 사용할 때:**
- 연관 엔티티의 데이터가 필요하지 않은 경우
- 단순히 조건 검색용으로만 사용하는 경우

**페치 조인을 사용할 때:**
- 연관 엔티티의 데이터를 반드시 사용해야 하는 경우
- N+1 문제를 방지하고 성능을 최적화해야 하는 경우

페치 조인은 JPA의 핵심 성능 최적화 기법 중 하나로, 실무에서 매우 중요한 개념입니다.
