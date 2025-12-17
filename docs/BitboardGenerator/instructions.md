# Java Bitboard Move Generator Implementation Checklist

Implementing a bitboard engine in Java requires using `long` primitives, as Java's `long` is a signed 64-bit integer. You will rely heavily on the `Long` class's bitwise utilities.

---

### Phase 1: Java Environment & Bitboard Basics
* [x] **Primitive Selection**: Use `long` for all bitboards.
    * *Note: Java lacks unsigned longs, but bitwise operations (`&`, `|`, `^`, `<<`) treat the bits the same. Use `Long.divideUnsigned()` if you ever need to divide.*
* [x] **Bitboard Utilities**: Map the following built-in methods to your engine:
    * `Long.numberOfTrailingZeros(long b)`: Equivalent to `get_lsb`.
    * `Long.bitCount(long b)`: Useful for evaluation and counting pieces.
* [x] **The "Wrap-Around" Masks**: Define constant masks to prevent pieces from "jumping" sides during shifts:
    ```java
    final long FILE_A = 0x0101010101010101L;
    final long FILE_H = 0x8080808080808080L;
    final long FILE_AB = FILE_A | (FILE_A << 1);
    final long FILE_GH = FILE_H | (FILE_H >>> 1);
    ```

### Phase 2: Precomputing Leaper Attacks
* [x] **Knight Attacks**: Use a `long[64]` array. Populate it at startup using shifts:
    ```java
    long spot = 1L << square;
    long moves = ((spot << 17) & ~FILE_A) | ((spot << 10) & ~FILE_AB) | ... // add all 8 directions
    ```
* [x] **King Attacks**: Use a `long[64]` array. Populate using 8-way shifts around the central bit.

### Phase 2b: create interface to chesslib for better testing
* [x] **Check generated moves against validity of a real chess game** use for this step the library com.github.bhlangonijr chesslib.
* [x] **Add chesslib tests** Add simple moves that are valid in the chesslib in the tests. use a new testclass named ChessLibVsGeneratorValidityTest.

### Phase 3: Set-wise Pawn Generation
* [ ] **White Pawn Logic**:
    * `(pawns << 8) & empty`: Single push.
    * `((push1) << 8) & empty & RANK_4`: Double push.
    * `(pawns << 7) & ~FILE_H & blackPieces`: Capture Right.
    * `(pawns << 9) & ~FILE_A & blackPieces`: Capture Left.
* [ ] **Black Pawn Logic**: Mirror the shifts using the unsigned right shift operator `>>>`.

### Phase 4: Sliding Pieces (Java Implementation)
* [ ] **Magic Bitboard Tables**: Since Java manages memory differently, pre-allocate large `long[]` arrays for your attack tables to avoid `OutOfMemoryError` or excessive Garbage Collection during search.
* [ ] **Magic Hashing**:
    ```java
    public long getRookAttacks(int square, long occupancy) {
        occupancy &= rookMasks[square];
        int index = (int)((occupancy * rookMagics[square]) >>> (64 - rookShiftBits[square]));
        return rookTable[square][index];
    }
    ```


### Phase 5: The Move Loop & Legalization
* [ ] **Move Encoding**: Represent moves as a simple `int` to save memory.
    * Bits 0-5: Source square.
    * Bits 6-11: Destination square.
    * Bits 12-15: Special flags (Promotion, Castling, En Passant).
* [ ] **Make/Unmake Move**: Implement a method to update the bitboards.
    * *Tip: Use XOR (`^`) to toggle piece positions. `bitboard ^= (1L << from) | (1L << to);`*
* [ ] **Square Attacked Test**: Write a function `isSquareAttacked(int sq, int attackerColor)`. This is essential for determining if a move leaves the king in check.

---

### Phase 6: Performance & Testing
* [ ] **Perft Testing**: Implement a **Perft** (Performance Test) function. This recursively counts moves to a specific depth and compares them against known values (like the "Starting Position" or "KiwiPete" position).
* [ ] **Inlining**: Ensure your bit manipulation methods are `final` or `static` to encourage the JVM's Just-In-Time (JIT) compiler to inline them.