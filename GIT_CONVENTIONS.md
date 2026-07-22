# Git Naming Conventions — Branches & Commits

Этот документ описывает соглашения по именованию веток и оформлению сообщений коммитов, используемые в этом проекте. Они помогают поддерживать понятную историю изменений, удобную для людей и автоматизации (генерация changelog, CI/CD и т.д.).

---

## 📌 Общие правила веток

### 🔹 Формат имен
Ветки должны называться в формате:

```
<type>/<optional-issue-id>-<short-description>
```

Где:
- `<type>` — тип работы (см. ниже).
- `optional-issue-id` — номер задачи/тикета при наличии (например `PROJ-123`).
- `<short-description>` — короткое, понятное в **kebab-case** (строчные, дефисы).

**Примеры:**
```
feature/PROJ-42-add-login
bugfix/98-fix-header-overflow
hotfix/urgent-fix-typo-in-api
docs/update-readme
```

### 🔹 Типы веток
| Префикс | Назначение |
|---------|------------|
| feature/ | Новая функциональность |
| bugfix/ | Исправление ошибки |
| hotfix/ | Срочное исправление |
| release/ | Подготовка релиза |
| refactor/ | Рефакторинг |
| docs/ | Документация |
| test/ | Тесты |
| chore/ | Вспомогательные задачи |

---

## 📌 Сообщения коммитов — Conventional Commits

Используется спецификация Conventional Commits:
https://www.conventionalcommits.org/en/v1.0.0/

### Формат
```
<type>([optional-scope]): <short description>
```

### Примеры
```
feat(auth): add JWT login support
fix(api): handle null response
docs(readme): update installation steps
```

---

## 🧠 Типы коммитов
| Тип | Описание |
|-----|----------|
| feat | Новая функциональность |
| fix | Исправление ошибки |
| docs | Документация |
| style | Форматирование |
| refactor | Рефакторинг |
| perf | Оптимизация |
| test | Тесты |
| chore | Вспомогательные задачи |
| ci | CI/CD |
| build | Сборка |

---

## ✨ Рекомендации
- Используйте глаголы в императиве: add, fix, update
- Заголовок ≤ 50 символов
- Один коммит — одно логическое изменение
