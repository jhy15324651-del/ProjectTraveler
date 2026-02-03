package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.QuizOption;

import java.util.List;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {

    List<QuizOption> findByQuestionIdOrderBySortOrderAsc(Long questionId);

    List<QuizOption> findByQuestionIdAndIsCorrectTrue(Long questionId);
}
