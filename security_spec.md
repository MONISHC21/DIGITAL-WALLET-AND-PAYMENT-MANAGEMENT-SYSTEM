# DigiWallet Firestore Security Specification

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every document under `/users/{userId}` and its subcollections (`transactions`, `expenses`, `budgets`) is strictly isolated to `request.auth.uid == userId`.
2. **Identity Integrity**: Every document's `userId` or `ownerId` field must match `request.auth.uid` and `userId` on creation and remain immutable on update.
3. **Monetary Correctness**: All monetary fields (`demoBalancePaise`, `amountPaise`, `limitPaise`) must be integer values within safe bounds (`>= 0` for balance, `> 0 && <= 1000000000` for transaction/expense/budget amounts).
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid server timestamps (`is timestamp && <= request.time`), and `createdAt` is strictly immutable across updates.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Profile Read**: `GET /users/alice_123` with `auth == null` -> REJECTED.
2. **Cross-User Profile Read (PII Leak)**: `GET /users/bob_456` as `alice_123` -> REJECTED.
3. **Identity Spoofing on Profile Create**: `CREATE /users/alice_123` with `userId: "bob_456"` -> REJECTED.
4. **Ghost Field Shadow Update**: `UPDATE /users/alice_123` adding `"isAdmin": true` -> REJECTED via `hasOnly()`.
5. **Negative Demo Balance Poisoning**: `UPDATE /users/alice_123` with `demoBalancePaise: -5000` -> REJECTED.
6. **Zero/Negative Transaction Amount**: `CREATE /users/alice_123/transactions/tx1` with `amountPaise: 0` -> REJECTED.
7. **Invalid Transaction Type Enum**: `CREATE /users/alice_123/transactions/tx1` with `transactionType: "HACK"` -> REJECTED.
8. **Cross-User Transaction List**: `LIST /users/bob_456/transactions` as `alice_123` -> REJECTED.
9. **Immutable CreatedAt Tampering**: `UPDATE /users/alice_123/expenses/exp1` modifying `createdAt` -> REJECTED.
10. **Oversized String DoW Attack**: `CREATE /users/alice_123/expenses/exp1` with 5000-char `note` -> REJECTED.
11. **Invalid Budget Limit**: `CREATE /users/alice_123/budgets/b1` with `limitPaise: -100` -> REJECTED.
12. **OwnerId Mutation on Budget Update**: `UPDATE /users/alice_123/budgets/b1` changing `ownerId` -> REJECTED.
