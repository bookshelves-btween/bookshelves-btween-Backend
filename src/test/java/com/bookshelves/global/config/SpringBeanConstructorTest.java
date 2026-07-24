package com.bookshelves.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

class SpringBeanConstructorTest {

  @Test
  void springBeansWithMultipleConstructorsHaveAnExplicitInjectionOrDefaultConstructor() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

    List<String> invalidBeanClasses =
        scanner.findCandidateComponents("com.bookshelves").stream()
            .map(beanDefinition -> beanDefinition.getBeanClassName())
            .filter(className -> className != null)
            .map(this::loadClass)
            .filter(this::hasAmbiguousConstructors)
            .map(Class::getName)
            .sorted()
            .toList();

    assertThat(invalidBeanClasses)
        .as("여러 생성자를 가진 Spring Bean은 주입 생성자를 @Autowired로 지정하거나 기본 생성자를 제공해야 합니다.")
        .isEmpty();
  }

  private Class<?> loadClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException("Spring Bean 클래스를 불러오지 못했습니다: " + className, exception);
    }
  }

  private boolean hasAmbiguousConstructors(Class<?> beanClass) {
    Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
    if (constructors.length <= 1) {
      return false;
    }

    boolean hasDefaultConstructor =
        Arrays.stream(constructors).anyMatch(constructor -> constructor.getParameterCount() == 0);
    long autowiredConstructorCount =
        Arrays.stream(constructors)
            .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
            .count();

    return !hasDefaultConstructor && autowiredConstructorCount != 1;
  }
}
