import java.util.Scanner;

class Circle {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter radius: ");
        double r = sc.nextDouble();

        double area = Math.PI * r * r;

        System.out.println("Area of circle = " + area);
    }
}