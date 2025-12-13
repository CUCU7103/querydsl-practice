package com.querydsl.querydslpractice;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.querydsl.querydslpractice.entity.QMember.*;
import static com.querydsl.querydslpractice.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.querydslpractice.dto.MemberDto;
import com.querydsl.querydslpractice.dto.UserDto;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.querydslpractice.entity.Member;
import com.querydsl.querydslpractice.entity.QMember;
import com.querydsl.querydslpractice.entity.QTeam;
import com.querydsl.querydslpractice.entity.Team;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }


    @Test
    void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery(
                "select m from Member m " +
                    "where m.username = :username", Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    void startQueryDsl() {

//        QMember m = new QMember("m");

	/*	Member findMember = queryFactory
			.select(m)
			.from(m)
			.where(m.username.eq("member1"))
			.fetchOne();
*/
        Member findMember = queryFactory
            .select(
                member) // member를 static으로 바로 설정한다. (import com.querydsl.querydslpractice.entity.QMember;)
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();
        Assertions.assertNotNull(findMember);
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    void search() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.between(10, 30)))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    void searchAndParam() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"),
                member.age.eq(10))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam2() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.ne("member1"),
                member.age.eq(30))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member3");

    }

    @Test
    void searchAndParam3() {
        List<Member> findMember = queryFactory
            .selectFrom(member)
            .where(member.age.between(10, 30))
            .fetch();

        assertThat(findMember.get(0).getUsername()).isEqualTo("member1");
        assertThat(findMember.get(1).getUsername()).isEqualTo("member2");
        assertThat(findMember.get(2).getUsername()).isEqualTo("member3");
    }


    @Test
    void resultFetch() {
        /* List<Member> fetch = queryFactory
            .selectFrom(member)
            .fetch();

         Member fetchOne = queryFactory
             .selectFrom(member)
             .fetchOne();

         Member fetchFirst = queryFactory
             .selectFrom(member)
             .fetchFirst();*/

        QueryResults<Member> results = queryFactory
            .selectFrom(member)
            .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

    }

    /**
     * 회원 정렬 순서 1. 회원 나이 내림차순 2. 회원 이름 올림차순 단 2에서 회원 이름이 없으면 마지막에 출력
     */
    @Test
    void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    // 페이징 처리
    @Test
    void paging1() {
        List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetch();

        assertThat(result.size()).isEqualTo(2);
    }


    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    // 집합

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory.
            select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
            )
            .from(member)
            .fetch();

        // dto로 뽑아오는 방법을 많이 사용한다.
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);


    }

    /**
     *
     * 팀의 이름과 각 팀의 평균연령을 구해라
     *
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    // 조인
    @Test
    void join() {
        // arrange
        List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");

        // assert
    }

    /**
     * 세타 조인 회원의 이름이 팀 이름과 같은 회원을 조회 연관관계가 없이 조인이 가능함.
     */
    @Test
    void theta_join() {
        // arrange
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        // act

        List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

        // assert
        assertThat(result)
            .extracting("username")
            .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회 JPQL: select m , t from member m left join
     * m.team t on t.name = 'teamA'
     *
     */

    // on절 조인
    @Test
    void join_on_filtering() {
        // arrange
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("teamA"))
            // .join(member.team, team) .. inner join on 절의 경우에는 where 절을 사용한다 (inner join 한정)
            // .where(team.name.eq("teamA"))
            .fetch();

        // act
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        // assert
    }


    /**
     * 연관관계가 없는 엔티티 외부 조인 회원과 팀 이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() {
        // arrange
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        // act

        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team) // member.team이 아닌 그냥 team을 넣어버린다.
            .on(member.username.eq(team.name))
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            // 결과
            // tuple = [Member(id=1152, username=member1, age=10), null]
            // tuple = [Member(id=1153, username=member2, age=20), null]
            // tuple = [Member(id=1154, username=member3, age=30), null]
            // tuple = [Member(id=1155, username=member4, age=40), null]
            // tuple = [Member(id=1156, username=teamA, age=0), Team(id=1152, name=teamA)]
            // tuple = [Member(id=1157, username=teamB, age=0), Team(id=1153, name=teamB)]
            // tuple = [Member(id=1158, username=teamC, age=0), null]
        }
    }

    // fetch Join

    @PersistenceUnit
    EntityManagerFactory emf;

    // fetch join
    // sql에서 제공하는 기능은 아니다
    // sql 조인을 활용해서 연관된 엔티티를 한번에 조회하는 기능
    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        // 현재 Team은 fetch = Lazy로 되어져있기 때문에 Member 조회시 Team은 조회되지 않습니다.
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();


    }

    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        // 현재 Team은 fetch = Lazy로 되어져있기 때문에 Member 조회시 Team은 조회되지 않습니다.
        Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isTrue();


    }


    /// 서브 쿼리 나이가 가장 많은 회원 조회
    @Test
    void subQuery() {

        // arrange
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                select(memberSub.age.max())
                    .from(memberSub)

            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(40);

    }


    /// 나이가 평균 이상인 회원 조회
    @Test
    void subQueryGoe() {

        // arrange
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.avg())
                    .from(memberSub)

            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(30, 40);

    }


    @Test
    void subQueryIn() {

        // arrange
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(20, 30, 40);

    }


    ///  select 절에서 서브쿼리 사용
    @Test
    void selectSubQuery() {
        // arrange
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
            .select(member.username,
                select(memberSub.age.avg())
                    .from(memberSub))
            .from(member)
            .fetch();

        // act
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        // assert
    }



    /// case 문

    @Test
    void basicCase() {

        List<String> result = queryFactory
            .select(member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        // act

        // assert
    }


    @Test
    void complexCase() {
        // arrange
       List<String> result = queryFactory.select(new CaseBuilder().
               when(member.age.between(0,20)).then("0~20살")
            .when(member.age.between(21,30)).then("21~30살")
            .otherwise("기타"))
            .from(member)
            .fetch();
        // act

        for(String s : result){
            System.out.println("s = " + s);
        }

        // assert
    }


    //  상수
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
            .select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();


        for (Tuple tuple : result){
            System.out.printf("tuple = " + tuple);
        }

    }

    @Test
    public void concat(){
      List<String> result =  queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    ///  프로젝션
    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();

        for (String s : result ){
            System.out.println("s = " + s);
        }
    }


    @Test
    public void tupleProjection() {
        // arrange
        List<Tuple> result = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();
        // act
        for (Tuple tuple : result){
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }


        // assert
    }
/*
    @Test
    public void findDtoByJPQL() {
        // arrange
        em.createQuery("select new querydsl-practics.dto.MemberDto(m.username, m.age) from Member m" , MemberDto.class);
        // act

        // assert
    }*/
    @Test
    void findDtoBySetter() {
        // arrange
        List<MemberDto> result = queryFactory.select(Projections.bean(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
        // act
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
        
        // assert
    }

    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
        // act
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();
        // act
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }



///  프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결방안
    @Test
    void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"),

                // 서브쿼리 사용
                ExpressionUtils.as(JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub), "age")

            ))
            .from(member)
            .fetch();
        // act
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }


}
