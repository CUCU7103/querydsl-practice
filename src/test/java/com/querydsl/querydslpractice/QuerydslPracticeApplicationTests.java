package com.querydsl.querydslpractice;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.querydslpractice.entity.Hello;
import com.querydsl.querydslpractice.entity.QHello;

import jakarta.persistence.EntityManager;

@SpringBootTest
class QuerydslPracticeApplicationTests {


	@Autowired
	EntityManager em;

	@Test
	@Transactional
	void contextLoads() {

		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");

		Hello result = query
			.selectFrom(qHello)
			.fetchOne();

		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());

	}

}
