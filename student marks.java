import java.util.Scanner;

class Student {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter marks: ");
        int marks = sc.nextInt();

        if (marks > 90) {
            System.out.println("Grade: A");
            System.out.println("Result: Pass");
        } else if (marks >= 35) {
            System.out.println("Result: Pass");
        } else {
            System.out.println("Result: Fail");
        }
    }
}